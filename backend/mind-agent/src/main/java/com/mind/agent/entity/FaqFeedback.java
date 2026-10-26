package com.mind.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("faq_feedback")
public class FaqFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long faqId;
    private Long studentId;
    /** 1 有用 0 无用 */
    private Integer useful;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFaqId() { return faqId; }
    public void setFaqId(Long faqId) { this.faqId = faqId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Integer getUseful() { return useful; }
    public void setUseful(Integer useful) { this.useful = useful; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
