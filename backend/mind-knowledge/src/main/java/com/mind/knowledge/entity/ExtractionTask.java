package com.mind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("extraction_task")
public class ExtractionTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long docId;
    private String title;
    /** A=教案 B=讲义 C=纯知识点 */
    private String docType;
    /** CREATED/PARSING/EXTRACTING/MATCHING/PENDING_CONFIRM/CONFIRMED/PUBLISHED/OFFLINE/ROLLED_BACK/FAILED/CANCELLED */
    private String status;
    private Integer version;
    private String docText;
    private String resultJson;
    private String failReason;
    /** 本次任务新建节点的 Neo4j elementId 列表（JSON 数组），回滚时精确删除用 */
    private String createdNodeIds;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDocText() { return docText; }
    public void setDocText(String docText) { this.docText = docText; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getCreatedNodeIds() { return createdNodeIds; }
    public void setCreatedNodeIds(String createdNodeIds) { this.createdNodeIds = createdNodeIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
