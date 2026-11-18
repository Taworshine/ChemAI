package com.mind.mobile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 错题本（知识图谱：错题-错因/考查→知识点） */
@TableName("mobile_wrong_question")
public class MobileWrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String subject;
    private String conceptName;
    private String wrongReason;
    private String content;
    private String imagePath;
    private Integer wrongCount;
    private Long createBy;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getConceptName() { return conceptName; }
    public void setConceptName(String conceptName) { this.conceptName = conceptName; }
    public String getWrongReason() { return wrongReason; }
    public void setWrongReason(String wrongReason) { this.wrongReason = wrongReason; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
