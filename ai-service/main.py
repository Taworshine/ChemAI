"""
思维赋能教学辅助系统 - Python AI 服务（FastAPI, 端口 8000）
负责：文档解析（PDF/Word）、DeepSeek LLM 调用、苏格拉底式追问生成
"""
from fastapi import FastAPI
from routers import extract, chat, faq

app = FastAPI(title="Mind Empower AI Service", version="1.0.0")

app.include_router(extract.router)
app.include_router(chat.router)
app.include_router(faq.router)


@app.get("/api/health")
def health():
    from services import llm
    return {"ok": True, "llm_enabled": llm.USE_LLM}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
