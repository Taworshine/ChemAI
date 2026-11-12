"""
LLM 调用封装 + 无 Key 时的"思维路径引导引擎"。

模型供应商切换（写入 ai-service\.env 或系统环境变量）：
  LLM_PROVIDER=deepseek  默认。DEEPSEEK_API_KEY / DEEPSEEK_BASE_URL / DEEPSEEK_MODEL（旧配置仍兼容）
  LLM_PROVIDER=kimi      Kimi（Moonshot，OpenAI 兼容接口）
                         KIMI_API_KEY（或通用 LLM_API_KEY）
                         可选覆盖：LLM_BASE_URL / LLM_MODEL（默认 kimi-k2-0905-preview）

不配置任何 Key 时自动走内置引导引擎（可完整演示）。

引导引擎设计：
  - Java 端把 Neo4j 中该题型的思维路径（步骤描述 + 每步提示 hint + 当前步骤）传入 context
  - 引擎维护对话状态：评估学生回答（推进 / 停滞 / 索要答案），
    推进则进入下一步，停滞 2 次给该步 hint，全程不直接给答案
"""
import os
import json
import re
import time
import random

# 自动加载 ai-service\.env（存在才生效），让 Key 不用写进系统环境变量。
# override=True：.env 优先于系统环境变量，避免机器上残留的旧 KIMI_API_KEY 抢先生效。
try:
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env"), override=True)
except Exception:
    pass

# ---- 供应商预设：provider 名 → (Key 环境变量名, 默认 base_url, 默认模型) ----
PROVIDER = os.getenv("LLM_PROVIDER", "deepseek").lower()
_PRESETS = {
    "deepseek": ("DEEPSEEK_API_KEY", "https://api.deepseek.com", "deepseek-chat"),
    "kimi": ("KIMI_API_KEY", "https://api.moonshot.cn/v1", "kimi-k2-0905-preview"),
}
_KEY_ENV, _DEFAULT_BASE, _DEFAULT_MODEL = _PRESETS.get(PROVIDER, _PRESETS["deepseek"])

# 优先级：供应商专属 Key > 通用 LLM_API_KEY > 旧 DEEPSEEK_API_KEY
API_KEY = os.getenv(_KEY_ENV) or os.getenv("LLM_API_KEY") or os.getenv("DEEPSEEK_API_KEY")
BASE_URL = os.getenv("LLM_BASE_URL") or os.getenv("DEEPSEEK_BASE_URL") or _DEFAULT_BASE
MODEL = os.getenv("LLM_MODEL") or os.getenv("DEEPSEEK_MODEL") or _DEFAULT_MODEL

USE_LLM = bool(API_KEY)


def _client():
    from openai import OpenAI
    # 本环境 openai SDK 默认连接的认证头会被网关异常拒绝（401），
    # 显式传入 httpx2.Client 后正常——这里固定显式构造。
    # timeout 给足：reasoning 模型思考耗时长，太短会被掐断
    try:
        import httpx2 as httpx
        http = httpx.Client(timeout=150)
        return OpenAI(api_key=API_KEY, base_url=BASE_URL, timeout=150, http_client=http)
    except ImportError:
        return OpenAI(api_key=API_KEY, base_url=BASE_URL, timeout=150)


def _build_messages(prompt: str, history: list = None) -> list:
    """构造 OpenAI messages：system + 历史对话 + 当前 user prompt。
    历史只取最近 20 条（约 10 轮），避免上下文过长撑爆 token。"""
    messages = [{"role": "system", "content": "你是苏格拉底式教学助手，只输出要求内容。"}]
    if history:
        messages.extend(history[-20:])
    messages.append({"role": "user", "content": prompt})
    return messages


def chat_completion(prompt: str, history: list = None, max_tokens: int = 2000) -> str:
    """调用 LLM 生成文本；429 限流时退避重试，其余失败返回 None 交给引导引擎。
    history: 历史对话列表 [{"role": "user"|"assistant", "content": "..."}]，会注入 messages。
    max_tokens 需给 reasoning 模型留出思考空间，否则会 empty content。"""
    if not USE_LLM:
        return None
    messages = _build_messages(prompt, history)
    last_err = None
    for attempt, wait in ((0, 0), (1, 6), (2, 15)):  # 免费档 RPM 很低，限流时等待重试
        if wait:
            time.sleep(wait)
        try:
            resp = _client().chat.completions.create(
                model=MODEL,
                messages=messages,
                max_tokens=max_tokens,
            )
            content = (resp.choices[0].message.content or "").strip()
            if content:
                return content
            last_err = "empty content (reasoning 可能占满 token)"
        except Exception as e:
            last_err = e
    _log_fail(last_err)
    return None


def _log_fail(last_err) -> None:
    """失败原因落盘，便于排查（容器/后台运行时 stdout 不可见）"""
    try:
        with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "_llm.log"), "a", encoding="utf-8") as f:
            f.write(f"[{time.strftime('%H:%M:%S')}] model={MODEL} base={BASE_URL} 调用失败，降级规则引擎: {last_err}\n")
    except Exception:
        pass
    print(f"[llm] 调用失败，降级规则引擎: {last_err}")


def chat_completion_stream(prompt: str, history: list = None, max_tokens: int = 2000, limit: int = 300):
    """流式调用 LLM，逐段 yield 文本增量（已按 limit 截断，保证用户所见即最终入库内容）。
    history: 历史对话列表，会注入 messages。
    首次 yield 前完成限流重试；全部失败时静默返回（调用方走规则兜底）。"""
    if not USE_LLM:
        return
    messages = _build_messages(prompt, history)
    last_err = None
    for attempt, wait in ((0, 0), (1, 6), (2, 15)):
        if wait:
            time.sleep(wait)
        try:
            stream = _client().chat.completions.create(
                model=MODEL,
                messages=messages,
                max_tokens=max_tokens,
                stream=True,
            )
            total = 0
            for evt in stream:
                if not evt.choices:
                    continue
                delta = evt.choices[0].delta.content or ""
                if not delta:
                    continue
                if total >= limit:
                    return
                if total + len(delta) > limit:
                    delta = delta[:limit - total]
                total += len(delta)
                yield delta
            return
        except Exception as e:
            # 已在输出途中失败：不重试（避免重复段落），直接交给规则兜底
            if attempt > 0 or True:
                last_err = e
                break
    _log_fail(last_err)


def _socratic_prompt(message: str, context: dict) -> str:
    path_lines = "\n".join(f"{i + 1}. {p}" for i, p in enumerate(context.get("path") or []))
    consecutive_stuck_count = context.get("consecutive_stuck_count", 0)
    total_stuck_count = context.get("total_stuck_count", 0)
    consecutive_answer_requests = context.get("consecutive_answer_requests", 0)
    return f"""你是苏格拉底式教学助手。基于思维路径：
{path_lines or '（无）'}
当前进行到第 {context.get('current_step', 0) + 1} 步。
学生回答：{message}
历史卡点：{context.get('misconception') or '（无）'}
当前连续停滞次数：{consecutive_stuck_count}
本轮累计未推进次数：{total_stuck_count}
连续索要答案次数：{consecutive_answer_requests}

【第一步：判断学生状态】
A. 推进——学生在尝试回答、提出自己的思路，哪怕不完整或有错误
B. 索要答案——学生明确要求直接给答案、给步骤，或说“不会，你告诉我”
C. 停滞——学生表示不知道、给出“嗯/不知道/随便”类回应，或重复上一轮的困惑
D. 偏离——学生聊与当前题目无关的内容
E. 已理解——学生正确回答了导师上一轮提出的具体问题，或准确复述了当前这一步的推理逻辑，明确表示“懂了/明白了/会了”，且回答内容与当前步骤相符

判断 E 的关键：学生这一轮的回答必须实质命中导师上一轮问的那个点（给出正确的关系、正确的判断或正确的复述），而不是只说“懂了”却答不上来。
- 只要学生给出的判断在学科上正确、且答到了导师问的那个点，就判 E；不要因为句子短、没把细节说全而降级为 A 或 C。
- 只说“懂了/明白了”但没有任何实质内容 → 不算 E，仍按 C（停滞）处理，请学生说具体一点。
- 答非所问、答错、含糊带过 → 不算 E。

【判断学生答案对错时的学科核对】
- 化合价升高 = 失电子 = 被氧化，该元素所在的物质作还原剂（还原剂被氧化）。
- 化合价降低 = 得电子 = 被还原，该元素所在的物质作氧化剂（氧化剂被还原）。
学生表述只要符合上述定义即为正确，不要因为“剂”字读起来别扭就判错。

【E 判定示例】
- 导师上一轮问：“化合价升高的元素被氧化还是被还原？它所在的物质是氧化剂还是还原剂？”
  学生答：“被氧化，所以它所在的物质是还原剂。” → 判 E（正确命中）。
- 学生答：“被还原，所以是氧化剂。” → 不判 E（答错，按 C 处理，指出方向反了）。
- 学生答：“我懂了。” → 不判 E（无实质内容，按 C 处理）。
- 导师上一轮问：“反应物里哪种元素的化合价会变？”
  学生答：“锰从正七价变成正二价，氯从负一价变成零价。” → 判 E（标注正确且完整）。

【第二步：E 状态优先处理】
若判定为 E，直接输出收尾回复，并跳过第三、第四步：
- 明确肯定学生已经掌握（如“对，就是这个思路，你已经掌握了”）。
- 用一两句话总结本步/本题的关键点，帮学生固化。
- 不再提出任何新问题，不再追问，回复中不得出现问号。
- 收尾语可选一句：可以进入下一题，或自己再做一道同类题巩固。
- 若这轮是兜底交付后的验证，学生验证成功同样按 E 收尾，并视为该知识点已掌握。

【第三步：判断是否进入兜底交付】
如果本轮累计未推进次数 ≥3 ，或连续索要答案次数 ≥ 2 且引导无效，进入兜底交付模式，跳过第四步，直接执行：
- 完整、清晰地给出正确答案和关键步骤（这是唯一允许给答案的情形）。
- 答案后立刻要求学生完成一步验证：用自己的话复述最关键的一步，或做一道同类型最小变式。
- 学生验证成功后，按 E 状态收尾，标记该知识点掌握、触发勋章。

【第四步：非 E 且未进入兜底时，按状态选择回应策略】
- A（推进）：肯定合理部分，顺着学生思路追问下一步。不因答案不完整就改问更基础的问题。
- B（索要答案）：绝不直接给答案，但必须降低追问的抽象层级——拆成更小的一步，或给一个指向性提示（如“先看反应物里哪种元素化合价会变”）。禁止用“你觉得呢”“再想想”这类空泛追问回应索要答案。
- C（停滞）：若连续停滞次数 ≥ 1（本次为连续第 2 次及以上），必须给出该步提示（非答案），不得再追问；若首次停滞，换一种更具体、更小的问法，不重复上一轮句式。
- D（偏离）：一句话温和拉回当前步骤，然后继续。

【输出格式（强制）】
1. 纯文本输出，禁止使用任何 Markdown 语法：不得出现 **、*、#、`、>、- 开头的列表符、|表格| 等。
2. 需要分点时，用“1.”“2.”“3.”或“第一、第二”这类纯文本序号，每条单独一行。
3. 需要强调时，用中文引号“”或直接靠语序强调，不要用加粗、斜体或反引号。
4. 不要输出分隔线、代码块、表情符号。

【硬性约束】
1. 除兜底交付模式外，任何情况下禁止直接给出答案或完整解题步骤。
2. 非兜底模式下每次回复尽量不超过 80 字，如果已经超过，你也要保证逻辑完整。
3. 语言亲切鼓励，符合高中生认知水平。
4. 使用中文。
5. 必须输出状态分类（以供测试）：回复的第一行只输出一个状态字母（只能是 A/B/C/D/E 之一），空一行后再输出正文，不要解释判断依据。
6. 状态为 E 时，回复中禁止出现任何问号，禁止再接新的追问或新的思考题，必须是收尾语气。

现在生成回复："""


def socratic_reply_stream(message: str, history: list, context: dict):
    """流式版 socratic_reply。
    先逐段 yield ("delta", 文本增量)；最后 yield ("result", 完整结果 dict)。
    LLM 不可用/失败时，result 为规则引擎兜底（一次性给出，不再分段）。"""
    # 计算停滞/索要答案计数器并注入 context（Java 端不传，自行统计）
    counters = _compute_stuck_counters(history)
    ctx = dict(context)
    ctx.update(counters)

    # 连续索要答案 ≥2 次 或 累计未推进 ≥3 次 → 进入兜底交付模式
    # 本轮若学生已在推进（可能已理解，对应状态 E），不强制兜底给答案，交给 prompt 判断
    need_fallback = (counters["consecutive_answer_requests"] >= 2 or counters["total_stuck_count"] >= 3) \
        and _classify(message, history) != "progress"
    prompt = _fallback_answer_prompt(message, ctx) if need_fallback else _socratic_prompt(message, ctx)

    full = []
    for delta in chat_completion_stream(prompt, history):
        full.append(delta)
        yield "delta", delta
    content = "".join(full).strip()
    if content:
        yield "result", {"reply": content, "misconception_type": ctx.get("misconception"),
                         "next_step": 0, "engine": PROVIDER}
        return
    r = rule_reply(message, history, ctx)
    r["engine"] = "rule-based (LLM 调用失败或 Key 无效，已降级)"
    yield "result", r


# =====================================================================
#                        思维路径引导引擎
# =====================================================================

# 直接索要答案 / 放弃 的信号
GIVE_UP = ["直接告诉我", "直接给", "告诉我答案", "答案是什么", "别问了", "不想做了", "太难了",
           "放弃", "不做了", "抄答案", "给答案", "说答案"]
# 停滞信号
STUCK = ["不知道", "不会", "不懂", "没思路", "想不出来", "不明白", "不清楚", "毫无头绪", "懵了"]
# 推进信号（学生在尝试：给出数值/式子/判断/复述概念）
PROGRESS = ["所以", "那么", "应该", "因为", "等于", "就是", "我猜", "我觉得", "试试"]

PRAISE = ["很好，思路很清晰！", "不错，就是这样思考！", "对的，继续保持！", "很好，你已经摸到门道了！"]
PRAISE_STEP = [
    "很好！那我们进入下一步：",
    "非常好！接下来试试这个：",
    "漂亮！下一步我们来解决它：",
]
ENCOURAGE = [
    "别急，我们一步一步来～",
    "没关系，再观察一下已知条件。",
    "差一点点了，换个角度想想？",
]
REFUSE_ANSWER = [
    "答案先保密～自己解出来的才记得牢！我们先回到当前这一步：",
    "直接给答案就失去锻炼的机会啦！来，看这步：",
]

DEFAULT_QUESTION = "先别急着算～你能说说这题考查的是哪个知识点、给了哪些条件吗？"


def _classify(message: str, history: list) -> str:
    """评估学生最新回答的状态：progress / stuck / giveup / neutral"""
    text = message.strip()
    if any(g in text for g in GIVE_UP):
        return "giveup"
    if any(s in text for s in STUCK):
        return "stuck"
    # 停滞判断：最近两轮都缺少推进信号且很短
    recent_user = [h.get("content", "") for h in history if h.get("role") == "user"][-2:]
    short_and_empty = len(text) <= 6 and not any(c.isdigit() for c in text) and not any(k in text for k in PROGRESS)
    if short_and_empty and any(any(s in r for s in STUCK) for r in recent_user):
        return "stuck"
    # 推进信号：数字/公式/关键连接词/较长分析
    if any(k in text for k in PROGRESS) or any(c.isdigit() for c in text) or len(text) >= 15:
        return "progress"
    return "neutral"


def _compute_stuck_counters(history: list) -> dict:
    """从对话历史统计连续停滞/索要答案次数，用于判断是否进入兜底交付。
    Java 端不传这些字段，所以在 ai-service 侧自行计算。"""
    user_msgs = [h.get("content", "") for h in history if h.get("role") == "user"]
    if not user_msgs:
        return {"consecutive_stuck_count": 0, "consecutive_answer_requests": 0, "total_stuck_count": 0}

    # 从最近一条往前数连续停滞
    consecutive_stuck = 0
    for i in range(len(user_msgs) - 1, -1, -1):
        if _classify(user_msgs[i], []) == "stuck":
            consecutive_stuck += 1
        else:
            break

    # 从最近一条往前数连续索要答案
    consecutive_answer = 0
    for i in range(len(user_msgs) - 1, -1, -1):
        if _classify(user_msgs[i], []) == "giveup":
            consecutive_answer += 1
        else:
            break

    # 累计未推进次数（停滞 + 索要 + 中性）
    total_not_progress = sum(1 for m in user_msgs if _classify(m, []) in ("stuck", "giveup", "neutral"))

    return {
        "consecutive_stuck_count": consecutive_stuck,
        "consecutive_answer_requests": consecutive_answer,
        "total_stuck_count": total_not_progress,
    }


def _fallback_answer_prompt(message: str, context: dict) -> str:
    """兜底交付模式 prompt：学生多次无法推进，强制给出答案和关键步骤。"""
    path_lines = "\n".join(f"{i + 1}. {p}" for i, p in enumerate(context.get("path") or []))
    hints = context.get("hints") or []
    current_step = context.get("current_step", 0)
    current_hint = hints[current_step] if current_step < len(hints) else ""
    return f"""你是苏格拉底式教学助手。学生已经多次无法推进，现在进入【兜底交付模式】。

思维路径：
{path_lines or '（无）'}
当前步骤：第 {current_step + 1} 步
该步提示：{current_hint or '（无）'}
学生最后说：{message}

请执行以下操作：
1. 完整、清晰地给出这道题的正确答案和关键步骤（这是唯一允许给答案的情形）。
2. 答案后立刻要求学生完成一步验证：用自己的话复述最关键的一步，或做一道同类型最小变式。
3. 语气亲切，不要让学生觉得被放弃。

直接输出回复内容，不要解释。"""


def _ask_step(step: str, hint: str = None, stuck: bool = False, avoid: set = None) -> str:
    if stuck and hint:
        return f"提示一下：{hint}。顺着这个方向，你能试着做一做吗？"
    probes = [
        f"针对「{step}」这一步，你想先从哪里入手？",
        f"说到「{step}」，题目里有哪些条件能派上用场？",
        f"你能试着完成「{step}」吗？先写下你的想法～",
        f"要完成「{step}」，你会先写出什么？",
    ]
    if avoid:
        fresh = [p for p in probes if p not in avoid]
        if fresh:
            probes = fresh
    q = random.choice(probes)
    if hint:
        q += f"（小提示：{hint}）"
    return q


def _echo_fragment(message: str) -> str:
    """提取学生回答中的关键片段（化学式/数值/术语），用于复述式反馈。"""
    # 化学式 / 带数字的表达式 / 术语片段
    patterns = [r'[A-Z][a-z]?\d*(?:\([^)]+\))?\d*', r'[^\s，。？！,;；]{0,8}\d+(?:\.\d+)?[^\s，。？！,;；]{0,6}']
    found = []
    for p in patterns:
        for m in re.findall(p, message):
            if 1 < len(m) <= 14 and m not in found and not m.isdigit():
                found.append(m)
            if len(found) >= 2:
                break
        if len(found) >= 2:
            break
    return "、".join(found[:2])


def _recent_questions(history: list) -> set:
    """本会话已发出的追问，用于去重。"""
    return {h.get("content", "") for h in history if h.get("role") == "assistant"}


def _finish(path: list) -> str:
    return ("你已经走完这条思维路径的全部步骤啦！最后请你自己总结一遍：" +
            "→".join(path) + "。能独立总结出来，这题就真正属于你了！")


def rule_reply(message: str, history: list, context: dict) -> dict:
    path = context.get("path") or []
    hints = context.get("hints") or []
    current_step = context.get("current_step") or 0
    misconception = context.get("misconception")

    # 无思维路径上下文时：按关键词做学科级启发
    if not path:
        return _subject_fallback(message, history)

    # 兜底交付：连续索要答案 ≥2 次 或 累计未推进 ≥3 次 → 直接给答案 + 要求验证
    counters = _compute_stuck_counters(history)
    if counters["consecutive_answer_requests"] >= 2 or counters["total_stuck_count"] >= 3:
        step_idx = min(current_step, len(path) - 1)
        step = path[step_idx]
        hint = hints[step_idx] if step_idx < len(hints) else None
        answer = hint if hint else f"这道题的核心是「{step}」，按题目条件逐步推导即可得到结果。"
        return {"reply": f"好的，我直接告诉你：{answer}\n\n现在请你用自己的话复述一下这个思路，确认你理解了。",
                "misconception_type": misconception, "next_step": step_idx}

    user_turns = [h for h in history if h.get("role") == "user"]
    state = _classify(message, history)
    # 首轮提问不视为"推进"，避免跳过第一步
    if not user_turns and state == "progress":
        state = "neutral"
    step_idx = min(current_step, len(path) - 1)
    step = path[step_idx]
    hint = hints[step_idx] if step_idx < len(hints) else None

    avoid = _recent_questions(history)

    # 1. 索要答案 → 温柔拒绝并拉回当前步骤
    if state == "giveup":
        return {"reply": (random.choice(REFUSE_ANSWER) + _ask_step(step, avoid=avoid))[:110],
                "misconception_type": misconception, "next_step": step_idx}

    # 2. 停滞 → 上一轮也停滞（连续 2 次无法推进）则给该步阶梯提示
    prev_stuck = bool(user_turns) and _classify(user_turns[-1].get("content", ""), []) == "stuck"
    if state == "stuck":
        if prev_stuck:
            return {"reply": _ask_step(step, hint, stuck=True)[:110],
                    "misconception_type": "CONCEPT_UNCLEAR", "next_step": step_idx}
        return {"reply": (random.choice(ENCOURAGE) + _ask_step(step, avoid=avoid))[:110],
                "misconception_type": "CONCEPT_UNCLEAR", "next_step": step_idx}

    # 3. 有推进 → 复述学生的关键内容 + 肯定 + 进入下一步 / 收尾
    if state == "progress":
        frag = _echo_fragment(message)
        if step_idx >= len(path) - 1:
            return {"reply": _finish(path)[:110], "misconception_type": None, "next_step": step_idx}
        nxt = step_idx + 1
        echo = f"你提到{frag}，" if frag else ""
        reply = (random.choice(PRAISE_STEP) + echo +
                 _ask_step(path[nxt], hints[nxt] if nxt < len(hints) else None, avoid=avoid))
        return {"reply": reply[:110], "misconception_type": None, "next_step": nxt}

    # 4. 中性回答 → 在当前步骤继续探问
    return {"reply": _ask_step(step, hint if random.random() < 0.5 else None, avoid=avoid)[:110],
            "misconception_type": misconception, "next_step": step_idx}


# -------------- 学科级关键词启发（无路径时降级） --------------
SUBJECT_RULES = [
    (["化学", "配平", "化合价", "方程式", "离子", "摩尔", "原电池", "平衡"],
     "这题的关键概念是什么？能先写出相关的定义或公式吗？", "CONCEPT_UNCLEAR"),
    (["数学", "函数", "导数", "切线", "三角", "单调", "数列", "向量"],
     "先画个图或写个式子试试看～从已知条件能直接推出什么？", "LOGIC_ERROR"),
    (["物理", "力", "加速度", "动量", "速度", "电磁", "电路"],
     "先确定研究对象，把已知量和要求的量列出来，找找用哪个规律？", "LOGIC_ERROR"),
    (["生物", "细胞", "遗传", "基因", "呼吸", "光合"],
     "这个过程分几个阶段？每个阶段的场所和产物分别是什么？", "CONCEPT_UNCLEAR"),
    (["语文", "文言文", "翻译", "作文", "诗歌", "阅读"],
     "把它放回原文语境里，联系上下文推断一下试试？", "CONCEPT_UNCLEAR"),
    (["英语", "从句", "时态", "语态", "单词", "语法"],
     "先找出句子的主干，再看空缺处在从句中充当什么成分？", "CONCEPT_UNCLEAR"),
    (["历史", "革命", "意义", "背景", "影响"],
     "从政治、经济、思想几个维度分别想一想？", "LOGIC_ERROR"),
    (["地理", "风", "气压", "洋流", "气候", "地形"],
     "先定位纬度/海陆位置，再联想对应的分布规律？", "CONCEPT_UNCLEAR"),
    (["政治", "经济", "市场", "哲学", "文化"],
     "回忆相关原理的定义，再逐条对照材料？", "CONCEPT_UNCLEAR"),
]


def _subject_fallback(message: str, history: list) -> dict:
    text = message.lower()
    for keywords, question, mis in SUBJECT_RULES:
        if any(k in text for k in keywords):
            prefix = random.choice(PRAISE + [""])
            return {"reply": (prefix + question)[:90], "misconception_type": mis, "next_step": 0}
    idx = len([h for h in history if h.get("role") == "user"]) % len(ENCOURAGE)
    return {"reply": (ENCOURAGE[idx] + DEFAULT_QUESTION)[:90],
            "misconception_type": None, "next_step": 0}


def socratic_reply(message: str, history: list, context: dict) -> dict:
    """优先 LLM，失败/无 Key 时降级引导引擎。（非流式版，保留给内部调用）"""
    # 计算停滞/索要答案计数器并注入 context
    counters = _compute_stuck_counters(history)
    ctx = dict(context)
    ctx.update(counters)

    # 连续索要答案 ≥2 次 或 累计未推进 ≥3 次 → 兜底交付
    # 本轮若学生已在推进（可能已理解，对应状态 E），不强制兜底给答案，交给 prompt 判断
    need_fallback = (counters["consecutive_answer_requests"] >= 2 or counters["total_stuck_count"] >= 3) \
        and _classify(message, history) != "progress"
    prompt = _fallback_answer_prompt(message, ctx) if need_fallback else _socratic_prompt(message, ctx)

    content = chat_completion(prompt, history)
    if content:
        return {"reply": content[:300], "misconception_type": ctx.get("misconception"), "next_step": 0,
                "engine": PROVIDER}
    r = rule_reply(message, history, ctx)
    r["engine"] = f"rule-based (LLM 调用失败或 Key 无效，已降级)"
    return r


def summarize_knowledge(history: list, context: dict) -> dict:
    """学生进入 E 状态（已理解）后，由 AI 总结本轮对话：
    返回 {"footprint": 简短知识点短语, "comment": 简练笔记, "subject": 学科}。
    LLM 不可用时降级为从思维路径取当前步骤。"""
    path = context.get("path") or []
    current_step = context.get("current_step", 0)

    # 取最近若干轮拼成对话文本，供 LLM 提炼
    dialog_lines = []
    for h in history[-8:]:
        role = "学生" if h.get("role") == "user" else "导师"
        dialog_lines.append(f"{role}：{h.get('content', '')}")
    dialog_text = "\n".join(dialog_lines)

    prompt = f"""下面是一段师生苏格拉底式对话，学生已理解当前知识点。请提炼三项内容：
1. footprint：用一个词或简短中文短语（不超过 12 个字）概括本轮的知识点，如"金属与强酸反应"。
2. comment：用一两句话（不超过 100 字）写一段学习笔记，点明关键规则或易错点。
3. subject：判断本轮对话所属学科，只能从以下选项中选一个：语文、数学、英语、物理、化学、生物、历史、地理、其他。

只输出 JSON，格式：{{"footprint": "...", "comment": "...", "subject": "..."}}，不要输出任何其他内容。

对话：
{dialog_text}"""
    content = chat_completion(prompt, max_tokens=400)
    if content:
        try:
            m = re.search(r"\{.*\}", content, re.S)
            data = json.loads(m.group(0))
            footprint = str(data.get("footprint", "")).strip()
            comment = str(data.get("comment", "")).strip()
            subject = str(data.get("subject", "")).strip()
            if footprint and comment:
                return {"footprint": footprint[:255], "comment": comment[:1000], "subject": subject[:20]}
        except Exception:
            pass
    # 降级：用思维路径当前步骤
    step = path[current_step] if current_step < len(path) else "本轮知识点"
    return {"footprint": step[:255], "comment": f"学生已掌握：{step}", "subject": "其他"}


def summarize_title(history: list, message: str) -> str:
    """首轮对话结束后，由 AI 用不超过 30 个字概括本对话主题，作为会话标题。
    history: 历史对话（不含当前这条用户消息），message: 当前用户消息。
    LLM 不可用时降级为取用户首条消息截断。"""
    # 拼出已发生的对话文本（历史 + 当前用户消息）
    dialog_lines = []
    for h in history[-8:]:
        role = "学生" if h.get("role") == "user" else "导师"
        dialog_lines.append(f"{role}：{h.get('content', '')}")
    dialog_lines.append(f"学生：{message}")
    dialog_text = "\n".join(dialog_lines)

    prompt = f"""下面是一段师生对话的开头，请用一句不超过 30 个字的中文短语概括这段对话的主题，作为会话标题。
要求：只输出标题本身，不要加引号、序号或任何解释性文字。

对话：
{dialog_text}"""
    content = chat_completion(prompt, max_tokens=120)
    if content:
        title = content.strip()
        # 去掉可能的首尾引号、markdown 标记
        title = title.strip('"\'`“”‘’').strip()
        # 只取第一行
        title = title.splitlines()[0].strip() if title else title
        if title:
            return title[:30]
    # 降级：取用户首条消息截断
    fallback = message.strip().splitlines()[0].strip() if message else "新对话"
    return fallback[:30]


def extract_knowledge(text: str, doc_type: str = "A") -> dict:
    """LLM 信息抽取（按文档类型出不同 Schema，带 confidence 与 quote 溯源）；
    无 Key 时用关键词统计降级。"""
    if doc_type == "C":
        # C 类：纯知识点批量录入
        schema = ('{"concepts": [{"name": "知识点名称", "subject": "学科", "grade": "高一|高二|高三", '
                  '"description": "一句话描述", "keywords": ["关键词"], '
                  '"prerequisites": ["前置概念名，可空数组"], "confidence": 0.0, "quote": "原文摘录≤60字"}]}')
        requirement = "concepts ≥ 2 个；name 用教材通用名；confidence 为 0~1 自评；quote 必须抄自原文。"
    else:
        # A/B 类：教案 / 讲义
        schema = ('{"concepts": [{"name":"","subject":"","grade":"","description":"",'
                  '"keywords":[],"confidence":0.0,"quote":"原文摘录≤60字"}], '
                  '"question_types": [{"name":"","subject":"","difficulty":"基础|中等|较难",'
                  '"typical_example":"","confidence":0.0}], '
                  '"thinking_paths": [{"question_type":"对应题型名","steps":[{"order":1,"description":"",'
                  '"hint":""}],"confidence":0.0}], '
                  '"misconceptions": [{"description":"","related_concept":"","frequency":"high|mid|low",'
                  '"confidence":0.0,"quote":""}]}')
        requirement = "concepts ≥ 3 个，thinking_paths ≥ 1 条且 steps ≥ 2 步；confidence 为 0~1 自评。"

    prompt = f"""从以下高中教学文本中抽取知识，只输出 JSON（不要输出其他内容）：
{schema}

要求：{requirement}

文本：
{text[:3000]}"""
    # 抽取要输出完整 JSON，reasoning 模型会先消耗思考 token，上限给足
    content = chat_completion(prompt, max_tokens=8000)
    if content:
        try:
            m = re.search(r"\{.*\}", content, re.S)
            data = json.loads(m.group(0))
            if data.get("concepts"):
                data["engine"] = PROVIDER
                return _normalize_extract(data, doc_type)
        except Exception:
            pass
    # ---- 关键词降级抽取（confidence 统一 0.3，全部进入人工核对）----
    CONCEPTS = ["氧化还原反应", "化合价", "离子反应", "物质的量", "化学平衡",
                "原电池", "电解质", "化学反应速率", "函数", "导数", "牛顿运动定律",
                "动量", "细胞呼吸", "遗传规律", "定语从句", "文言文阅读", "辛亥革命",
                "大气环流", "市场经济", "元素周期律"]
    found = [c for c in CONCEPTS if c in text]
    data = {
        "concepts": [{"name": c, "subject": "化学", "grade": "高一",
                      "description": f"文本中涉及「{c}」相关内容", "keywords": [c],
                      "prerequisites": [], "confidence": 0.3, "quote": ""} for c in found[:6]],
        "engine": "rule-based (LLM 调用失败或 Key 无效，已降级)",
    }
    if doc_type != "C":
        data["question_types"] = [{"name": "概念辨析题", "subject": "化学", "difficulty": "基础",
                                   "typical_example": "", "confidence": 0.3}]
        data["thinking_paths"] = [{"question_type": "概念辨析题", "confidence": 0.3,
                                   "steps": [{"order": 1, "description": "识别概念关键词", "hint": "先圈出题目中的概念"},
                                             {"order": 2, "description": "联系定义与公式", "hint": "回忆课本定义"},
                                             {"order": 3, "description": "举例验证", "hint": "举一个反例试试"}]}]
        data["misconceptions"] = [{"description": "概念混淆", "related_concept": "", "frequency": "mid",
                                   "confidence": 0.3, "quote": ""}]
    if not data["concepts"]:
        data["concepts"] = [{"name": "核心概念", "subject": "化学", "grade": "高一",
                             "description": "（规则抽取未识别出具体概念，请教师手动编辑）",
                             "keywords": [], "prerequisites": [], "confidence": 0.1, "quote": ""}]
    return data


def cluster_questions(questions: list) -> list:
    """LLM 语义聚类：把同义/近义的学生问题归组，返回 [[索引...], ...]；失败返回 None。
    用于教师端高频问题工作台"智能归并"，减少重复解答。索引必须全覆盖且不重复才有效。"""
    if not USE_LLM or len(questions) < 2:
        return None
    numbered = "\n".join(f"{i}. {q}" for i, q in enumerate(questions))
    prompt = f"""下面是学生向老师提的问题列表（编号从 0 开始）。把语义相同或几乎相同的问题分到同一组。
只输出 JSON，格式：{{"groups": [[0, 3], [1], [2, 5]]}}
要求：
1. 只有确信是同一问题才归组，宁缺毋滥；独自成组的问题单独一组
2. 每个编号必须出现且只能出现一次，编号范围 0~{len(questions) - 1}
3. 不要输出任何其他内容

问题列表：
{numbered}"""
    # 问题多时分组 JSON 很长，reasoning 模型先消耗思考 token，上限给足
    content = chat_completion(prompt, max_tokens=8000)
    if not content:
        return None
    try:
        m = re.search(r"\{.*\}", content, re.S)
        data = json.loads(m.group(0))
        groups = data.get("groups")
        flat = sorted(i for g in groups for i in g)
        if flat != list(range(len(questions))):
            return None
        return groups
    except Exception:
        return None


def _normalize_extract(data: dict, doc_type: str) -> dict:
    """给缺失字段补默认值，保证下游 Java/前端拿到稳定结构。"""
    for c in data.get("concepts", []):
        c.setdefault("subject", "化学")
        c.setdefault("grade", "高一")
        c.setdefault("description", "")
        c.setdefault("keywords", [])
        c.setdefault("prerequisites", [])
        c.setdefault("confidence", 0.5)
        c.setdefault("quote", "")
    for q in data.get("question_types", []):
        q.setdefault("subject", "化学")
        q.setdefault("difficulty", "基础")
        q.setdefault("typical_example", "")
        q.setdefault("confidence", 0.5)
    for p in data.get("thinking_paths", []):
        p.setdefault("question_type", "")
        p.setdefault("confidence", 0.5)
        for i, s in enumerate(p.get("steps", [])):
            s.setdefault("order", i + 1)
            s.setdefault("description", "")
            s.setdefault("hint", "")
    for m in data.get("misconceptions", []):
        m.setdefault("related_concept", "")
        m.setdefault("frequency", "mid")
        m.setdefault("confidence", 0.5)
        m.setdefault("quote", "")
    return data
