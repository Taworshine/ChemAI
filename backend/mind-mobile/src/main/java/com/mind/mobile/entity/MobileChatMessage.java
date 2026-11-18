package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 会议聊天消息表 */
@TableName("mobile_chat_message")
public class MobileChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long userId;
    private String content;
    private String msgType;
    private Long replyTo;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }
    public Long getReplyTo() { return replyTo; }
    public void setReplyTo(Long replyTo) { this.replyTo = replyTo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
