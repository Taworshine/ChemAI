"""高频问题语义聚类接口：Java 后端调用，LLM 把同义问题归组供教师工作台合并。"""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

from services import llm

router = APIRouter()


class ClusterRequest(BaseModel):
    questions: List[str]


@router.post("/api/faq/cluster")
def cluster(req: ClusterRequest):
    groups = llm.cluster_questions(req.questions)
    if groups is None:
        # LLM 不可用或输出不可信：各自成组（前端提示走兜底）
        return {"ok": False, "groups": [[i] for i in range(len(req.questions))]}
    return {"ok": True, "groups": groups}
