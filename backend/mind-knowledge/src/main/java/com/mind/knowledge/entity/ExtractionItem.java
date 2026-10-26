package com.mind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("extraction_item")
public class ExtractionItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    /** concept / qtype / path / misconception */
    private String kind;
    /** 候选节点 JSON（大模型抽取结果） */
    private String payload;
    /** L1 精确 / L2 模糊 / null 无匹配 */
    private String matchLevel;
    private String matchNodeId;
    private String matchNodeLabel;
    private String matchNodeName;
    private Double similarity;
    /** PENDING / NEW / MERGE / RELATE / IGNORE */
    private String decision;
    private String editedPayload;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }
    public String getMatchNodeId() { return matchNodeId; }
    public void setMatchNodeId(String matchNodeId) { this.matchNodeId = matchNodeId; }
    public String getMatchNodeLabel() { return matchNodeLabel; }
    public void setMatchNodeLabel(String matchNodeLabel) { this.matchNodeLabel = matchNodeLabel; }
    public String getMatchNodeName() { return matchNodeName; }
    public void setMatchNodeName(String matchNodeName) { this.matchNodeName = matchNodeName; }
    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getEditedPayload() { return editedPayload; }
    public void setEditedPayload(String editedPayload) { this.editedPayload = editedPayload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
