package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 会议表 */
@TableName("mobile_meeting")
public class MobileMeeting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String meetingNo;
    private String title;
    private String subject;
    private String agenda;
    private Long hostId;
    private Long currentDocId;
    private Integer status;
    private Integer locked;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMeetingNo() { return meetingNo; }
    public void setMeetingNo(String meetingNo) { this.meetingNo = meetingNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getAgenda() { return agenda; }
    public void setAgenda(String agenda) { this.agenda = agenda; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public Long getCurrentDocId() { return currentDocId; }
    public void setCurrentDocId(Long currentDocId) { this.currentDocId = currentDocId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getLocked() { return locked; }
    public void setLocked(Integer locked) { this.locked = locked; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
