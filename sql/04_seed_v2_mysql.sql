-- =============================================================
-- V2 种子数据：多学科题库 + 练习记录
-- =============================================================
USE mind_empower;

INSERT INTO question_bank (subject, concept_name, question_type, content, answer, difficulty) VALUES
-- 化学
('化学','氧化还原反应','氧化还原方程式配平','配平：Cu + HNO3(稀) → Cu(NO3)2 + NO↑ + H2O','3Cu + 8HNO3 = 3Cu(NO3)2 + 2NO↑ + 4H2O','中'),
('化学','离子反应','离子方程式书写','写出 Ba(OH)2 与 H2SO4 反应的离子方程式','Ba2+ + 2OH- + 2H+ + SO4 2- = BaSO4↓ + 2H2O','中'),
('化学','物质的量','物质的量计算','标准状况下 22.4L CO2 的物质的量是多少？','n = V/Vm = 22.4/22.4 = 1 mol','易'),
('化学','化学平衡','平衡移动分析','对 N2(g)+3H2(g)⇌2NH3(g) △H<0，升高温度平衡向哪个方向移动？','逆反应方向（吸热方向）移动','难'),
-- 数学
('数学','函数','函数单调性','求函数 f(x)=x²-2x 的单调递增区间','对称轴 x=1，单调递增区间为 [1,+∞)','易'),
('数学','导数','导数求切线','求曲线 y=x³ 在点 (1,1) 处的切线方程','y′=3x²，k=3，切线方程 y=3x-2','中'),
('数学','三角函数','三角恒等变换','已知 sinα=3/5，α∈(π/2,π)，求 cosα','cosα = -4/5','中'),
-- 物理
('物理','牛顿运动定律','受力分析','质量 2kg 的物体受到 10N 水平拉力，摩擦力 4N，求加速度','a = (F-f)/m = (10-4)/2 = 3 m/s²','易'),
('物理','动量','动量守恒','质量 3kg、速度 4m/s 的小车与静止的 1kg 小车碰后共速，求共同速度','mv=(M+m)v′，v′=3×4/4=3 m/s','中'),
-- 生物
('生物','细胞呼吸','有氧呼吸过程','有氧呼吸三个阶段分别在哪里进行？','细胞质基质；线粒体基质；线粒体内膜','中'),
('生物','遗传规律','基因分离定律','Aa 自交后代中杂合子所占比例为多少？','1/2','易'),
-- 语文
('语文','文言文阅读','文言实词','解释"沛公军霸上"中"军"字的用法和意义','名词作动词，驻军、驻扎','中'),
-- 英语
('英语','定语从句','关系代词选择','This is the book ______ I bought yesterday. 填入适当的关系词','that / which','易'),
-- 历史
('历史','辛亥革命','历史意义','简述辛亥革命的历史意义（至少两点）','推翻清王朝，结束两千多年君主专制；建立资产阶级共和国；民主共和观念深入人心','中'),
-- 地理
('地理','大气环流','气压带风带','北半球中纬西风带的风向是？','西南风','易'),
-- 政治
('政治','市场经济','市场调节','市场调节的弊端有哪些？','自发性、盲目性、滞后性','易');

-- 练习记录示例（李思源 2 条、张雨桐 1 条）
INSERT INTO practice_record (student_id, subject, score, total, detail) VALUES
(2,'化学',3,4,'[{"questionId":1,"content":"配平：Cu + HNO3","userAnswer":"3Cu + 8HNO3 = 3Cu(NO3)2 + 2NO + 4H2O","rightAnswer":"3Cu + 8HNO3 = 3Cu(NO3)2 + 2NO↑ + 4H2O","correct":true},{"questionId":2,"content":"Ba(OH)2 与 H2SO4 离子方程式","userAnswer":"Ba2+ + SO4 2- = BaSO4↓","rightAnswer":"Ba2+ + 2OH- + 2H+ + SO4 2- = BaSO4↓ + 2H2O","correct":false},{"questionId":3,"content":"22.4L CO2 物质的量","userAnswer":"1 mol","rightAnswer":"n = 22.4/22.4 = 1 mol","correct":true},{"questionId":4,"content":"合成氨升温平衡移动","userAnswer":"逆反应方向","rightAnswer":"逆反应方向（吸热方向）移动","correct":true}]'),
(2,'数学',2,3,'[{"questionId":5,"content":"f(x)=x²-2x 单调递增区间","userAnswer":"[1,+∞)","rightAnswer":"[1,+∞)","correct":true},{"questionId":6,"content":"y=x³ 在 (1,1) 处切线","userAnswer":"y=3x-2","rightAnswer":"y=3x-2","correct":true},{"questionId":7,"content":"sinα=3/5 求 cosα","userAnswer":"4/5","rightAnswer":"-4/5","correct":false}]'),
(3,'化学',2,4,'[{"questionId":1,"content":"配平：Cu + HNO3","userAnswer":"Cu + 4HNO3 = Cu(NO3)2 + 2NO2 + 2H2O","rightAnswer":"3Cu + 8HNO3 = 3Cu(NO3)2 + 2NO↑ + 4H2O","correct":false},{"questionId":3,"content":"22.4L CO2 物质的量","userAnswer":"1 mol","rightAnswer":"1 mol","correct":true},{"questionId":4,"content":"合成氨升温平衡移动","userAnswer":"正反应方向","rightAnswer":"逆反应方向","correct":false},{"questionId":2,"content":"离子方程式","userAnswer":"Ba2+ + 2OH- + 2H+ + SO4 2- = BaSO4↓ + 2H2O","rightAnswer":"Ba2+ + 2OH- + 2H+ + SO4 2- = BaSO4↓ + 2H2O","correct":true}]');
