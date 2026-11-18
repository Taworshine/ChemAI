package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 会议纪要表 */
@TableName("mobile_summary")
public class MobileSummary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private String content;
    private Integer autoGenerate;
    private Integer isConfirmed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getAutoGenerate() { return autoGenerate; }
    public void setAutoGenerate(Integer autoGenerate) { this.autoGenerate = autoGenerate; }
    public Integer getIsConfirmed() { return isConfirmed; }
    public void setIsConfirmed(Integer isConfirmed) { this.isConfirmed = isConfirmed; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
