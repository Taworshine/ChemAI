package com.mind.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 学情画像表：记录学生在对话中掌握的知识点足迹与 AI 生成的学习笔记。
 * 表结构见 sql/07_student_profile.sql。
 */
@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生用户 ID（sys_user.id） */
    private Long studentId;

    /** 学科：语文/数学/英语/物理/化学/生物/历史/地理/其他 */
    private String subject;

    /** 简短概括的知识点，如：金属单质与强酸反应 */
    private String footprint;

    /** AI 生成的学习笔记，如：铁、锌等活泼金属与盐酸、稀硫酸反应生成盐和氢气 */
    private String comment;

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFootprint() {
        return footprint;
    }

    public void setFootprint(String footprint) {
        this.footprint = footprint;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
