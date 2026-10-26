"""启发式追问接口：Java 后端调用，生成苏格拉底式追问"""
import json as _json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List, Optional

from services import llm

router = APIRouter()


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatContext(BaseModel):
    path: Optional[List[str]] = []
    hints: Optional[List[str]] = []
    current_step: int = 0
    misconception: Optional[str] = None


class ChatRequest(BaseModel):
    message: str
    history: List[ChatMessage] = []
    context: Optional[ChatContext] = None


@router.post("/api/chat")
def chat(req: ChatRequest):
    history = [m.model_dump() for m in req.history]
    context = req.context.model_dump() if req.context else {}
    result = llm.socratic_reply(req.message, history, context)
    result["ok"] = True
    result["engine"] = result.get("engine") or llm.PROVIDER
    return result


@router.post("/api/chat/stream")
def chat_stream(req: ChatRequest):
    """SSE 流式追问：先逐段推 delta（LLM 边生成边推），最后推 result 完整结果。
    LLM 不可用时 result 为规则引擎兜底（一次性给出）。"""
    history = [m.model_dump() for m in req.history]
    context = req.context.model_dump() if req.context else {}

    def gen():
        for kind, payload in llm.socratic_reply_stream(req.message, history, context):
            if kind == "delta":
                yield f"event: delta\ndata: {_json.dumps({'text': payload}, ensure_ascii=False)}\n\n"
            else:
                payload = dict(payload)
                payload["ok"] = True
                payload["engine"] = payload.get("engine") or llm.PROVIDER
                yield f"event: result\ndata: {_json.dumps(payload, ensure_ascii=False)}\n\n"

    return StreamingResponse(gen(), media_type="text/event-stream")
