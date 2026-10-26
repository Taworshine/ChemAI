-- =============================================================
-- mind_empower V2：题库 + 练习记录（多学科）
-- =============================================================
USE mind_empower;

-- 题库表
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject VARCHAR(30) NOT NULL COMMENT '学科',
    concept_name VARCHAR(100) COMMENT '关联知识点',
    question_type VARCHAR(100) COMMENT '题型',
    content TEXT NOT NULL COMMENT '题目内容',
    answer TEXT NOT NULL COMMENT '参考答案',
    difficulty ENUM('易','中','难') DEFAULT '中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 练习记录表
CREATE TABLE IF NOT EXISTS practice_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    subject VARCHAR(30) NOT NULL,
    score INT COMMENT '答对题数',
    total INT COMMENT '总题数',
    detail JSON COMMENT '答题明细 [{questionId,content,userAnswer,rightAnswer,correct}]',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id)
);
