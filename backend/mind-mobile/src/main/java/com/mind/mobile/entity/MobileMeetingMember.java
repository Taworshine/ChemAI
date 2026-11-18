package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 会议成员表 */
@TableName("mobile_meeting_member")
public class MobileMeetingMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long userId;
    private String role;
    private Integer online;
    private LocalDateTime joinTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getOnline() { return online; }
    public void setOnline(Integer online) { this.online = online; }
    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }
}
