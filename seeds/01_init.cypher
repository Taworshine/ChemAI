// =============================================================
// Neo4j 5.26 初始化：约束 + 知识图谱种子数据（高中化学）
// =============================================================
CREATE CONSTRAINT concept_name IF NOT EXISTS FOR (c:Concept) REQUIRE c.name IS UNIQUE;
CREATE CONSTRAINT qt_id IF NOT EXISTS FOR (q:QuestionType) REQUIRE q.id IS UNIQUE;
CREATE CONSTRAINT path_id IF NOT EXISTS FOR (p:ThinkingPath) REQUIRE p.id IS UNIQUE;
CREATE CONSTRAINT mis_id IF NOT EXISTS FOR (m:Misconception) REQUIRE m.id IS UNIQUE;

// ---------- 概念节点（含画布坐标，便于 G6 展示） ----------
CREATE (c1:Concept {id: 1, name: '氧化还原反应', subject: '化学', grade: '高三', keywords: '化合价,电子转移,氧化剂,还原剂', description: '有电子转移（化合价升降）的化学反应', x: 300, y: 200});
CREATE (c2:Concept {id: 2, name: '化合价', subject: '化学', grade: '初三/高一', keywords: '标价,常见化合价', description: '元素原子得失电子或共用电子对的带电表现', x: 120, y: 80});
CREATE (c3:Concept {id: 3, name: '离子反应', subject: '化学', grade: '高一', keywords: '电离,离子方程式,拆写', description: '有离子参加或生成的化学反应', x: 300, y: 60});
CREATE (c4:Concept {id: 4, name: '物质的量', subject: '化学', grade: '高一', keywords: '摩尔,NA,气体摩尔体积', description: '以阿伏伽德罗常数为计数单位的物质的量', x: 520, y: 90});
CREATE (c5:Concept {id: 5, name: '化学平衡', subject: '化学', grade: '高二', keywords: '可逆反应,勒夏特列原理,平衡常数', description: '可逆反应中正逆反应速率相等的动态平衡状态', x: 520, y: 300});
CREATE (c6:Concept {id: 6, name: '原电池', subject: '化学', grade: '高二', keywords: '电极,电势,电子流向', description: '把化学能转化为电能的装置', x: 120, y: 320});
CREATE (c7:Concept {id: 7, name: '电解质溶液', subject: '化学', grade: '高一', keywords: '强弱电解质,电离平衡', description: '在水溶液中或熔融状态下能导电的化合物体系', x: 320, y: 400});
CREATE (c8:Concept {id: 8, name: '化学反应速率', subject: '化学', grade: '高二', keywords: 'v=Δc/Δt,活化能,催化剂', description: '单位时间内反应物浓度的减少或生成物浓度的增加', x: 700, y: 200});

// ---------- 概念前置关系 ----------
MATCH (a:Concept {name:'氧化还原反应'}), (b:Concept {name:'化合价'}) CREATE (a)-[:PREREQUISITE]->(b);
MATCH (a:Concept {name:'离子反应'}), (b:Concept {name:'电解质溶液'}) CREATE (a)-[:PREREQUISITE]->(b);
MATCH (a:Concept {name:'原电池'}), (b:Concept {name:'氧化还原反应'}) CREATE (a)-[:PREREQUISITE]->(b);
MATCH (a:Concept {name:'化学平衡'}), (b:Concept {name:'化学反应速率'}) CREATE (a)-[:PREREQUISITE]->(b);

// ---------- 题型节点 ----------
CREATE (q1:QuestionType {id: 1, name: '氧化还原方程式配平', difficulty: '中', typicalExample: 'Cu + HNO3 → Cu(NO3)2 + NO + H2O'});
CREATE (q2:QuestionType {id: 2, name: '氧化剂与还原剂判断', difficulty: '易', typicalExample: '在 Cl2 + H2O ⇌ HCl + HClO 中指出氧化剂和还原剂'});
CREATE (q3:QuestionType {id: 3, name: '离子方程式书写与判断', difficulty: '中', typicalExample: 'Ba(OH)2 与 H2SO4 反应的离子方程式'});
CREATE (q4:QuestionType {id: 4, name: '物质的量综合计算', difficulty: '中', typicalExample: '标准状况下 22.4L O2 含有的分子数'});
CREATE (q5:QuestionType {id: 5, name: '原电池电极判断', difficulty: '难', typicalExample: '铜锌原电池中判断正负极并写出电极反应式'});
CREATE (q6:QuestionType {id: 6, name: '化学平衡移动分析', difficulty: '难', typicalExample: '升高温度对 N2+3H2⇌2NH3 平衡的影响'});

// ---------- 思维路径节点 ----------
CREATE (p1:ThinkingPath {id: 1, stepOrder: 1, description: '标出变价元素在反应前后的化合价', hint: '常见元素化合价：O 通常为 -2，H 为 +1，金属看价态'});
CREATE (p2:ThinkingPath {id: 2, stepOrder: 2, description: '计算每个原子得失电子数及总数', hint: '失电子化合价升高，得电子化合价降低'});
CREATE (p3:ThinkingPath {id: 3, stepOrder: 3, description: '由最小公倍数确定变价物质系数', hint: '得失电子总数必须相等'});
CREATE (p4:ThinkingPath {id: 4, stepOrder: 4, description: '用原子守恒补齐其余物质并检查', hint: '最后数一遍每种原子的个数'});
CREATE (p5:ThinkingPath {id: 5, stepOrder: 1, description: '判断反应物在水溶液中的电离情况', hint: '强酸、强碱、可溶性盐可拆'});
CREATE (p6:ThinkingPath {id: 6, stepOrder: 2, description: '删去方程式两边相同的旁观离子', hint: '没有参与生成沉淀、气体或水的离子删掉'});
CREATE (p7:ThinkingPath {id: 7, stepOrder: 3, description: '检查电荷守恒与原子守恒', hint: '两边电荷总数相等吗？'});
CREATE (p8:ThinkingPath {id: 8, stepOrder: 1, description: '确定条件是否适用 22.4L/mol', hint: '必须是气体 + 标准状况（0℃、101kPa）'});
CREATE (p9:ThinkingPath {id: 9, stepOrder: 2, description: '由体积求物质的量 n = V/Vm', hint: 'n 是连接宏观与微观的桥梁'});
CREATE (p10:ThinkingPath {id: 10, stepOrder: 3, description: '由 n 求微粒数 N = n·NA', hint: 'NA ≈ 6.02×10^23 mol^-1'});

// ---------- 卡点（迷思概念）节点 ----------
CREATE (m1:Misconception {id: 1, description: '化合价标注错误', frequency: 68});
CREATE (m2:Misconception {id: 2, description: '电子得失不守恒', frequency: 55});
CREATE (m3:Misconception {id: 3, description: '把系数配成分数后忘记同乘', frequency: 32});
CREATE (m4:Misconception {id: 4, description: '强电解质与可拆物质混淆', frequency: 47});
CREATE (m5:Misconception {id: 5, description: '离子方程式电荷不守恒', frequency: 41});
CREATE (m6:Misconception {id: 6, description: '忽视 22.4L/mol 的适用条件（非气体或非标况）', frequency: 59});
CREATE (m7:Misconception {id: 7, description: 'NA 与摩尔质量单位混淆', frequency: 25});

// ---------- 关系：概念→题型 ----------
MATCH (c:Concept {name:'氧化还原反应'}), (q:QuestionType {id:1}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'氧化还原反应'}), (q:QuestionType {id:2}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'离子反应'}), (q:QuestionType {id:3}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'物质的量'}), (q:QuestionType {id:4}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'原电池'}), (q:QuestionType {id:5}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'化学平衡'}), (q:QuestionType {id:6}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);

// ---------- 关系：题型→思维路径 ----------
MATCH (q:QuestionType {id:1}), (p:ThinkingPath) WHERE p.id IN [1,2,3,4] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (q:QuestionType {id:3}), (p:ThinkingPath) WHERE p.id IN [5,6,7] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (q:QuestionType {id:4}), (p:ThinkingPath) WHERE p.id IN [8,9,10] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (q:QuestionType {id:2}), (p:ThinkingPath) WHERE p.id IN [1,2] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);

// ---------- 关系：思维路径→常见卡点 ----------
MATCH (p:ThinkingPath {id:1}), (m:Misconception {id:1}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:2}), (m:Misconception {id:2}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:3}), (m:Misconception {id:3}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:5}), (m:Misconception {id:4}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:7}), (m:Misconception {id:5}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:8}), (m:Misconception {id:6}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:10}), (m:Misconception {id:7}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
