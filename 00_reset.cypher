// 清空本系统的四类图谱节点（不动其他项目的数据）
MATCH (n) WHERE n:Concept OR n:QuestionType OR n:ThinkingPath OR n:Misconception DETACH DELETE n;
