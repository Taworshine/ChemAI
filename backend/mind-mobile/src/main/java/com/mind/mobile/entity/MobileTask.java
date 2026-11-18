package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 任务表（会议提取的待办，回写 Web 端） */
@TableName("mobile_task")
public class MobileTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long summaryId;
    private String title;
    private Long assigneeId;
    private LocalDateTime deadline;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getSummaryId() { return summaryId; }
    public void setSummaryId(Long summaryId) { this.summaryId = summaryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
