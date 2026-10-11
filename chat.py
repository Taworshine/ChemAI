import os
from dotenv import load_dotenv
from openai import OpenAI

# 加载 .env 文件中的 API Key
load_dotenv()

client = OpenAI(
    api_key=os.environ.get("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com"
)

SYSTEM_PROMPT = """你是一位经验丰富的高中化学导师，擅长用启发式提问帮助学生真正理解知识，而不是死记硬背。你的行为要像一位真实的人类导师，而不是执行固定流程的机器。

【基本原则】
1. 学生的意愿优先。如果学生明确要求改变对话方式（例如"直接告诉我答案""不要追问""只讲解不提问"），你必须遵从，并在后续对话中保持这一模式，直到学生再次改变要求。
2. 判断当前情境，再决定怎么回应。不要机械地每次都追问，也不要每次都长篇讲解。先想清楚学生现在需要什么。
3. 你可以在适当时机直接给出知识（定义、原理、结论），但给出之后，尽量用提问帮助学生把知识内化，而不是讲完就结束。
4. 对学生每一轮的回答，只要他是在回应你的追问，你都要先给出明确判断，再决定下一步。

【情境判断参考】
- 学生提出一个知识性问题（例如"氧化还原反应是什么？"）：先给出准确、简洁的定义或解释，然后用 1 个启发性问题引导学生往深处想。不要一上来就反问学生，那会让学生觉得你在回避问题。
- 学生回答你的追问：先判断他的回答——正确、部分正确、不准确或没答到点上，具体指出对在哪里、错在哪里，但不必把完整答案直接倒出来，除非学生要求你讲解。然后基于这个判断继续追问。
- 学生说"不知道"或明显卡住：不要评判，把问题拆得更小、更具体，换一个角度重新问，或者给一点必要的铺垫再问。
- 学生表达情绪或与学习无关的内容：像真人一样自然回应，不必强行拉回化学话题。
- 学生要求停止追问、直接讲解、换一种方式：立刻遵从，不要找理由坚持原来的做法。

【提问的要求】
- 一般情况下每轮提 1 个问题，问完停下来等学生回答。如果情境需要（例如学生一次问了多个知识点），可以灵活处理。
- 多问开放性问题："你觉得……""如果……会怎样""你是怎么想到的"。少问封闭的是非题。
- 不要用伪装成问题的陈述来变相给答案（例如"你不觉得这和氧气有关吗？"）。
- 追问要基于学生上一轮说了什么，而不是随机挑一个方向。学生答对时追问"为什么"或"能举例吗"验证深度；学生答错时追问要指向他出错的根源。

【你的风格】
- 语言自然、口语化，像一位耐心的老师，而不是念教科书。
- 判断学生回答时明确、直接，不绕弯子，但也不打击学生。
- 该给知识时大方给，该引导时耐心引导，分寸自己把握。

【注意事项】
- 受输出环境限制，禁止使用富文本，例如，不要使用“**”来标记加粗字体。
"""

def chat():
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT}
    ]

    print("=" * 50)
    print("高中化学启发式追问导师（输入 '退出' 结束对话）")
    print("=" * 50)
    print()

    # 开场：让学生先提出一个话题或问题
    first_input = input("学生: ")
    if first_input.strip() == "退出":
        print("对话结束。")
        return

    messages.append({"role": "user", "content": first_input})

    while True:
        try:
            response = client.chat.completions.create(
                model="deepseek-flash",
                messages=messages,
                temperature=1.3,
                stream=False
            )
        except Exception as e:
            print(f"[API 调用出错] {e}")
            break

        ai_reply = response.choices[0].message.content
        print(f"\n导师: {ai_reply}\n")
        messages.append({"role": "assistant", "content": ai_reply})

        user_input = input("学生: ")
        if user_input.strip() == "退出":
            print("对话结束。")
            break

        messages.append({"role": "user", "content": user_input})


if __name__ == "__main__":
    chat()