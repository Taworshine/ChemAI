package com.mind.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("faq")
public class Faq {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String questionText;
    private String conceptName;
    private Integer askCount;
    private String answer;
    private Long answeredBy;
    private LocalDateTime answeredAt;
    /** 0 待解答 1 已发布 */
    private Integer publishStatus;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getConceptName() { return conceptName; }
    public void setConceptName(String conceptName) { this.conceptName = conceptName; }
    public Integer getAskCount() { return askCount; }
    public void setAskCount(Integer askCount) { this.askCount = askCount; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Long getAnsweredBy() { return answeredBy; }
    public void setAnsweredBy(Long answeredBy) { this.answeredBy = answeredBy; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    public Integer getPublishStatus() { return publishStatus; }
    public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
