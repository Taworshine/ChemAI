-- =============================================================
-- 学情画像表（自 01_schema.sql 迁出并重构）
-- 幂等：可重复执行
-- 注意：若库中已存在旧版 student_profile（含 concept_mastery/thinking_radar），
--       需先 DROP TABLE student_profile; 再执行本脚本，否则建表会被跳过。
-- =============================================================
USE mind_empower;

-- 学情画像表：记录学生的学习足迹与 AI 生成的笔记
CREATE TABLE IF NOT EXISTS student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生用户ID（sys_user.id）',
    footprint VARCHAR(255) COMMENT '简短概括的知识点，如：金属单质与强酸反应',
    `comment` VARCHAR(1000) COMMENT 'AI 生成的笔记，如：铁、锌等活泼金属与盐酸、稀硫酸反应生成盐和氢气；注意铁生成亚铁盐，不是铁盐。',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id)
);

-- 样例数据（student_id 2~5 对应学号 2023001~2023004 的李思源、张雨桐、陈昊然、刘梓萱）
INSERT INTO student_profile (student_id, footprint, `comment`) VALUES
(2, '金属单质与强酸反应', '铁、锌等活泼金属与盐酸、稀硫酸反应生成盐和氢气；注意铁生成亚铁盐，不是铁盐。'),
(2, '氧化还原反应中的电子转移', '化合价升高即失电子被氧化，该物质作还原剂；化合价降低即得电子被还原，该物质作氧化剂。'),
(3, '离子方程式的书写规则', '先写正确的化学方程式，再把易溶且易电离的物质拆成离子，难溶物、气体、水、弱电解质保留化学式，最后配平原子和电荷。'),
(4, '胶体的丁达尔效应', '光束通过胶体时出现一条光亮的通路即为丁达尔效应，可用于区分胶体和溶液。'),
(5, '物质的量浓度计算', 'c = n / V，其中 V 为溶液体积（单位 L）。稀释前后溶质的物质的量不变，即 c1V1 = c2V2。');
