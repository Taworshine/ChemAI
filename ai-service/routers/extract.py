"""知识抽取接口：Java 后端调用，负责文档解析 + LLM 抽取"""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

from services.parser import parse_file
from services import llm

router = APIRouter()


class ExtractRequest(BaseModel):
    doc_id: int
    doc_type: Optional[str] = "A"
    file_path: Optional[str] = None
    text: Optional[str] = None


@router.post("/api/extract")
def extract(req: ExtractRequest):
    # 1. 获取文本：优先直接使用 Java 传来的文本，否则本地解析文件
    text = req.text or parse_file(req.file_path)
    if not text.strip():
        return {"ok": False, "error": "未提取到文本，请检查文件路径或格式"}

    # 2. LLM 信息抽取（按文档类型出 Schema；无 Key 自动降级规则抽取）
    result = llm.extract_knowledge(text, req.doc_type or "A")
    result["doc_id"] = req.doc_id
    result["ok"] = True
    result["engine"] = result.get("engine") or llm.PROVIDER
    return result
