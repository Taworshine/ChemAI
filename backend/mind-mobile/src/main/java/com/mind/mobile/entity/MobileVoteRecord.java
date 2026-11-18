package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 投票记录表 */
@TableName("mobile_vote_record")
public class MobileVoteRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long voteId;
    private Long meetingId;
    private Long userId;
    private String optionId;
    private Integer score;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVoteId() { return voteId; }
    public void setVoteId(Long voteId) { this.voteId = voteId; }
    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
