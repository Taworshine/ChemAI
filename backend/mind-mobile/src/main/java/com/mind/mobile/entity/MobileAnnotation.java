package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 批注表（手绘/文字/语音，相对坐标存储） */
@TableName("mobile_annotation")
public class MobileAnnotation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long docId;
    private Long userId;
    private String type;
    private Integer page;
    private String color;
    private Integer strokeWidth;
    private String drawData;
    private String content;
    private String voicePath;
    private Integer voiceSeconds;
    private Long replyTo;
    private Integer isResolved;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(Integer strokeWidth) { this.strokeWidth = strokeWidth; }
    public String getDrawData() { return drawData; }
    public void setDrawData(String drawData) { this.drawData = drawData; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVoicePath() { return voicePath; }
    public void setVoicePath(String voicePath) { this.voicePath = voicePath; }
    public Integer getVoiceSeconds() { return voiceSeconds; }
    public void setVoiceSeconds(Integer voiceSeconds) { this.voiceSeconds = voiceSeconds; }
    public Long getReplyTo() { return replyTo; }
    public void setReplyTo(Long replyTo) { this.replyTo = replyTo; }
    public Integer getIsResolved() { return isResolved; }
    public void setIsResolved(Integer isResolved) { this.isResolved = isResolved; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
