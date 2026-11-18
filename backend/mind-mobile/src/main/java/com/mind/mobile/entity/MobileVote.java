package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 投票表 */
@TableName("mobile_vote")
public class MobileVote {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long creatorId;
    private String title;
    private String voteType;
    private String options;
    private Integer anonymous;
    private LocalDateTime deadline;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVoteType() { return voteType; }
    public void setVoteType(String voteType) { this.voteType = voteType; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getAnonymous() { return anonymous; }
    public void setAnonymous(Integer anonymous) { this.anonymous = anonymous; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
