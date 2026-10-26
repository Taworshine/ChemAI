package com.mind.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("dialog_message")
public class DialogMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** USER / AI */
    private String role;
    private String content;
    private String misconceptionType;
    private Integer thinkingStep;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMisconceptionType() {
        return misconceptionType;
    }

    public void setMisconceptionType(String misconceptionType) {
        this.misconceptionType = misconceptionType;
    }

    public Integer getThinkingStep() {
        return thinkingStep;
    }

    public void setThinkingStep(Integer thinkingStep) {
        this.thinkingStep = thinkingStep;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
