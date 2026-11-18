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


def chat_completion(prompt: str, max_tokens: int = 2000) -> str:
    """调用 LLM 生成文本；429 限流时退避重试，其余失败返回 None 交给引导引擎。
    max_tokens 需给 reasoning 模型留出思考空间，否则会 empty content。"""
    if not USE_LLM:
        return None
    last_err = None
    for attempt, wait in ((0, 0), (1, 6), (2, 15)):  # 免费档 RPM 很低，限流时等待重试
        if wait:
            time.sleep(wait)
        try:
            resp = _client().chat.completions.create(
                model=MODEL,
                messages=[{"role": "system", "content": "你是苏格拉底式教学助手，只输出要求内容。"},
                          {"role": "user", "content": prompt}],
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


def chat_completion_stream(prompt: str, max_tokens: int = 2000, limit: int = 90):
    """流式调用 LLM，逐段 yield 文本增量（已按 limit 截断，保证用户所见即最终入库内容）。
    首次 yield 前完成限流重试；全部失败时静默返回（调用方走规则兜底）。"""
    if not USE_LLM:
        return
    last_err = None
    for attempt, wait in ((0, 0), (1, 6), (2, 15)):
        if wait:
            time.sleep(wait)
        try:
            stream = _client().chat.completions.create(
                model=MODEL,
                messages=[{"role": "system", "content": "你是苏格拉底式教学助手，只输出要求内容。"},
                          {"role": "user", "content": prompt}],
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
    return f"""你是苏格拉底式教学助手。基于思维路径：
{path_lines or '（无）'}
当前进行到第 {context.get('current_step', 0) + 1} 步。
学生回答：{message}
历史卡点：{context.get('misconception') or '（无）'}

要求：
1. 禁止直接给出答案或完整解题步骤
2. 通过提问引导学生思考，顺着当前这一步追问
3. 学生连续2次无法推进时，给该步的提示（非答案）
4. 每次回复不超过80字，语言亲切鼓励
5. 使用中文，符合高中生认知水平

只输出追问内容本身。"""


def socratic_reply_stream(message: str, history: list, context: dict):
    """流式版 socratic_reply。
    先逐段 yield ("delta", 文本增量)；最后 yield ("result", 完整结果 dict)。
    LLM 不可用/失败时，result 为规则引擎兜底（一次性给出，不再分段）。"""
    full = []
    for delta in chat_completion_stream(_socratic_prompt(message, context)):
        full.append(delta)
        yield "delta", delta
    content = "".join(full).strip()
    if content:
        yield "result", {"reply": content, "misconception_type": context.get("misconception"),
                         "next_step": 0, "engine": PROVIDER}
        return
    r = rule_reply(message, history, context)
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
    content = chat_completion(_socratic_prompt(message, context))
    if content:
        return {"reply": content[:90], "misconception_type": context.get("misconception"), "next_step": 0,
                "engine": PROVIDER}
    r = rule_reply(message, history, context)
    r["engine"] = f"rule-based (LLM 调用失败或 Key 无效，已降级)"
    return r


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
