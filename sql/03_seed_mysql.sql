-- =============================================================
-- mind_empower 演示种子数据
-- 密码均为 123456 的 SHA-256：8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- =============================================================
USE mind_empower;

-- 1. 用户：1 位教师 + 10 名学生（高三(2)班）
INSERT INTO sys_user (username, password, real_name, role, class_name) VALUES
('teacher', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '王老师', 'TEACHER', '高三(2)班'),
('2023001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '李思源', 'STUDENT', '高三(2)班'),
('2023002', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张雨桐', 'STUDENT', '高三(2)班'),
('2023003', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '陈昊然', 'STUDENT', '高三(2)班'),
('2023004', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '刘梓萱', 'STUDENT', '高三(2)班'),
('2023005', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '赵子墨', 'STUDENT', '高三(2)班'),
('2023006', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '孙艺涵', 'STUDENT', '高三(2)班'),
('2023007', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '周俊熙', 'STUDENT', '高三(2)班'),
('2023008', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '吴诗涵', 'STUDENT', '高三(2)班'),
('2023009', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '郑皓宇', 'STUDENT', '高三(2)班'),
('2023010', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '冯若曦', 'STUDENT', '高三(2)班');

-- 2. 教学文档示例（一份课件已"解析成功"，一份试卷待解析）
INSERT INTO edu_document (title, doc_type, file_path, parse_status, parse_result, upload_user_id) VALUES
('氧化还原反应（第一课时）课件', 'COURSEWARE', './uploads/氧化还原反应课件.pdf', 2,
 '{"concepts":["氧化还原反应","化合价","氧化剂与还原剂"],"question_types":["氧化还原方程式配平","氧化剂还原剂判断"],"thinking_paths":[{"name":"配平思维路径","steps":["标出变价元素化合价","计算电子得失总数","用最小公倍数配平变价","原子守恒补齐其余物质"]}],"misconceptions":["化合价标注错误","电子得失不守恒"]}', 1),
 ('高三化学第一次月考试卷', 'EXAM', './uploads/高三第一次月考.pdf', 0, NULL, 1),
 ('李思源错题本（9月）', 'ERROR_BOOK', NULL, 0, NULL, 2);

-- 3. 学情画像：已迁至 07_student_profile.sql（表结构与样例数据）

-- 4. 对话会话与消息示例（模拟学生与 AI 的苏格拉底式追问过程）
INSERT INTO dialog_session (student_id, question_type_id, status, summary) VALUES
(2, 1, 1, '配平 Cu + HNO3 → Cu(NO3)2 + NO + H2O；卡点在化合价标注，经 3 轮追问后独立完成'),
(5, 1, 0, '配平 KMnO4 与浓盐酸反应；卡在电子得失不守恒，提示后仍未解决'),
(6, 2, 1, '离子方程式书写：Ba(OH)2 与 H2SO4；卡点在拆写是否正确'),
(5, 3, 2, '物质的量计算：22.4L O2 的分子数；卡点在气体摩尔体积适用条件');

INSERT INTO dialog_message (session_id, role, content, misconception_type, thinking_step) VALUES
-- 会话1：李思源（成功 case）
(1, 'USER', '老师，Cu + HNO3 → Cu(NO3)2 + NO + H2O 这个方程式怎么配平？', NULL, 0),
(1, 'AI', '先别急着配平～你能先标出铜元素和氮元素在反应前后的化合价吗？', NULL, 1),
(1, 'USER', 'Cu 是 0 价，变成 Cu(NO3)2 里的 +2；HNO3 里的 N 是 +5，NO 里是 +2。', NULL, 1),
(1, 'AI', '很好！那铜一共失去几个电子？每个氮原子又得到几个电子？', NULL, 2),
(1, 'USER', 'Cu 失 2 个，N 得 3 个。', NULL, 2),
(1, 'AI', '得失电子数要相等，2 和 3 的最小公倍数是多少？试着给 Cu 和 NO 配上系数。', NULL, 3),
(1, 'USER', '最小公倍数 6，所以 Cu 配 3，NO 配 2，然后硝酸铜 3，再用 H 守恒配 H2O 是 4，HNO3 是 8。', NULL, 3),
(1, 'AI', '完全正确！3Cu + 8HNO3 = 3Cu(NO3)2 + 2NO↑ + 4H2O。你已经掌握了"标价→算得失→配系数→守恒检查"的完整路径！', NULL, 4),
-- 会话2：赵子墨（未解决 case，体现卡点诊断）
(2, 'USER', 'KMnO4 + HCl → KCl + MnCl2 + Cl2 + H2O 怎么配平？', NULL, 0),
(2, 'AI', '第一步永远是标价～标出 Mn 和 Cl 在反应前后的化合价试试？', NULL, 1),
(2, 'USER', 'Mn 从 +7 变 +2，Cl 从 -1 变 0。那是不是 KMnO4 配 1，MnCl2 配 1？', 'CONCEPT_UNCLEAR', 1),
(2, 'AI', 'Mn 对了！但注意：每个 Cl2 分子有 2 个 Cl 原子。1 个 Mn 得 5 个电子，需要几个 Cl 原子失电子来配对？', 'CONCEPT_UNCLEAR', 2),
(2, 'USER', '5 个？所以 Cl2 配 5/2？', 'CALC_ERROR', 2),
(2, 'AI', '思路对了！系数不能有分数，把它们同时乘以 2 再试试看？', 'CALC_ERROR', 3),
-- 会话3：孙艺涵（离子方程式）
(3, 'USER', 'Ba(OH)2 和 H2SO4 反应的离子方程式怎么写？', NULL, 0),
(3, 'AI', '先回忆一下：哪些物质在离子方程式中可以拆成离子形式？Ba(OH)2 属于哪一类？', NULL, 1),
(3, 'USER', '强酸强碱可溶性盐可以拆，Ba(OH)2 是强碱，可以拆。', NULL, 1),
(3, 'AI', '对！那拆开之后，哪些离子会真正参与反应生成沉淀或水？', NULL, 2),
(3, 'USER', 'Ba2+ 和 SO4 2- 生成 BaSO4 沉淀，H+ 和 OH- 生成水。', NULL, 2),
(3, 'AI', '非常棒！把它们组合起来就是完整的离子方程式了，你写一遍试试？', NULL, 3),
(3, 'USER', 'Ba2+ + 2OH- + 2H+ + SO4 2- = BaSO4↓ + 2H2O', NULL, 3),
(3, 'AI', '完全正确！这题的关键就是"先拆、再删旁观离子、后守恒检查"。', NULL, 4),
-- 会话4：赵子墨（已关闭）
(4, 'USER', '标准状况下 22.4L O2 含有多少个分子？', NULL, 0),
(4, 'AI', '看到 22.4L 你想到了哪个物理量？它的适用前提是什么？', NULL, 1),
(4, 'USER', '气体摩尔体积，标准状况下 1mol 气体是 22.4L。那答案是 NA 个？', 'CONCEPT_UNCLEAR', 1),
(4, 'AI', '很好！但要提醒你：22.4L/mol 只对"气体"且"标准状况"成立。O2 在标况下是什么状态？', 'CONCEPT_UNCLEAR', 2),
(4, 'USER', '气体，所以 1mol，NA 个分子，约 6.02×10^23。', NULL, 2);
