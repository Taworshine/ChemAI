package com.mind.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    /** JSON 字符串：{"氧化还原":0.85,...} */
    private String conceptMastery;
    /** JSON 字符串：{"概念理解":80,...} */
    private String thinkingRadar;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getConceptMastery() {
        return conceptMastery;
    }

    public void setConceptMastery(String conceptMastery) {
        this.conceptMastery = conceptMastery;
    }

    public String getThinkingRadar() {
        return thinkingRadar;
    }

    public void setThinkingRadar(String thinkingRadar) {
        this.thinkingRadar = thinkingRadar;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
