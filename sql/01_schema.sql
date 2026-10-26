-- =============================================================
-- mind_empower 数据库结构（MySQL 8.0）
-- 高中化学"思维赋能"教学辅助系统
-- =============================================================
CREATE DATABASE IF NOT EXISTS mind_empower DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mind_empower;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名/学号',
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role ENUM('TEACHER','STUDENT') NOT NULL,
    class_name VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 教学文档表
CREATE TABLE IF NOT EXISTS edu_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    doc_type ENUM('COURSEWARE','EXAM','ERROR_BOOK') NOT NULL,
    file_path VARCHAR(500) COMMENT '本地存储路径',
    parse_status TINYINT DEFAULT 0 COMMENT '0待解析 1解析中 2成功 3失败',
    parse_result JSON COMMENT 'Python服务返回的抽取结果',
    upload_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 对话会话表
CREATE TABLE IF NOT EXISTS dialog_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_type_id BIGINT,
    status TINYINT DEFAULT 0 COMMENT '0进行中 1已解决 2关闭',
    summary VARCHAR(500) COMMENT '会话总结（思维卡点）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 对话消息表
CREATE TABLE IF NOT EXISTS dialog_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    role ENUM('USER','AI') NOT NULL,
    content TEXT,
    misconception_type VARCHAR(50) COMMENT '卡点类型：CONCEPT_UNCLEAR/CALC_ERROR/LOGIC_ERROR',
    thinking_step INT COMMENT '当前思维路径步骤',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id)
);

-- 学情画像表
CREATE TABLE IF NOT EXISTS student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT UNIQUE,
    concept_mastery JSON COMMENT '{"氧化还原":0.85,"离子反应":0.72}',
    thinking_radar JSON COMMENT '{"概念理解":80,"逻辑推理":75,"计算准确":90,"迁移应用":65,"反思修正":70}',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
