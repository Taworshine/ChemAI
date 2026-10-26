// =============================================================
// V2：多学科知识图谱扩展（语数英物化生政史地）
// =============================================================

// ---------- 数学 ----------
CREATE (c10:Concept {id: 10, name: '函数', subject: '数学', grade: '高一', keywords: '定义域,值域,单调性,奇偶性', description: '两个非空数集之间的对应关系', x: 940, y: 120});
CREATE (c11:Concept {id: 11, name: '导数', subject: '数学', grade: '高三', keywords: '切线,极值,单调区间', description: '函数在某点处变化的快慢程度', x: 1140, y: 200});
CREATE (c12:Concept {id: 12, name: '三角函数', subject: '数学', grade: '高一', keywords: '正弦,余弦,诱导公式', description: '角度与比值之间的函数关系', x: 1040, y: 20});
CREATE (q10:QuestionType {id: 10, name: '函数单调性判断', difficulty: '易', subject: '数学', typicalExample: '求 f(x)=x²-2x 的单调递增区间'});
CREATE (q11:QuestionType {id: 11, name: '导数求切线方程', difficulty: '中', subject: '数学', typicalExample: '求 y=x³ 在点 (1,1) 处的切线方程'});
CREATE (q12:QuestionType {id: 12, name: '三角恒等变换', difficulty: '中', subject: '数学', typicalExample: '已知 sinα 求 cosα'});
CREATE (p11:ThinkingPath {id: 11, stepOrder: 1, description: '确定函数定义域', hint: '先看 x 能取什么值'});
CREATE (p12:ThinkingPath {id: 12, stepOrder: 2, description: '求导并找临界点', hint: '令 f′(x)=0 求解'});
CREATE (p13:ThinkingPath {id: 13, stepOrder: 3, description: '划分区间判断导数符号', hint: '导数正则增，负则减'});
CREATE (m10:Misconception {id: 10, description: '忽略定义域导致单调区间错误', frequency: 49});
CREATE (m11:Misconception {id: 11, description: '导数几何意义与物理意义混淆', frequency: 37});
MATCH (c:Concept {name:'导数'}), (b:Concept {name:'函数'}) CREATE (c)-[:PREREQUISITE]->(b);
MATCH (c:Concept {name:'函数'}), (q:QuestionType {id:10}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'导数'}), (q:QuestionType {id:11}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'三角函数'}), (q:QuestionType {id:12}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:10}), (p:ThinkingPath) WHERE p.id IN [11,13] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (q:QuestionType {id:11}), (p:ThinkingPath) WHERE p.id IN [12,13] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:11}), (m:Misconception {id:10}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:12}), (m:Misconception {id:11}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 物理 ----------
CREATE (c20:Concept {id: 20, name: '牛顿运动定律', subject: '物理', grade: '高一', keywords: 'F=ma,受力分析,超重失重', description: '力与运动关系的三大基本定律', x: 940, y: 340});
CREATE (c21:Concept {id: 21, name: '动量', subject: '物理', grade: '高二', keywords: '动量守恒,冲量,碰撞', description: '质量与速度的乘积，描述运动量的物理量', x: 1140, y: 420});
CREATE (q20:QuestionType {id: 20, name: '受力分析与牛顿第二定律', difficulty: '易', subject: '物理', typicalExample: '求水平面上物体的加速度'});
CREATE (q21:QuestionType {id: 21, name: '动量守恒应用', difficulty: '中', subject: '物理', typicalExample: '碰撞后共同速度的计算'});
CREATE (p20:ThinkingPath {id: 20, stepOrder: 1, description: '确定研究对象并隔离受力', hint: '先画受力示意图'});
CREATE (p21:ThinkingPath {id: 21, stepOrder: 2, description: '沿运动方向列牛顿第二定律', hint: 'F合 = ma'});
CREATE (p22:ThinkingPath {id: 22, stepOrder: 1, description: '判断系统动量是否守恒', hint: '合外力为零或内力远大于外力'});
CREATE (p23:ThinkingPath {id: 23, stepOrder: 2, description: '由动量守恒列式求解', hint: '碰前总动量 = 碰后总动量'});
CREATE (m20:Misconception {id: 20, description: '漏掉摩擦力或支持力方向判断错误', frequency: 52});
CREATE (m21:Misconception {id: 21, description: '动量守恒条件判断错误', frequency: 44});
MATCH (c:Concept {name:'动量'}), (b:Concept {name:'牛顿运动定律'}) CREATE (c)-[:PREREQUISITE]->(b);
MATCH (c:Concept {name:'牛顿运动定律'}), (q:QuestionType {id:20}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'动量'}), (q:QuestionType {id:21}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:20}), (p:ThinkingPath) WHERE p.id IN [20,21] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (q:QuestionType {id:21}), (p:ThinkingPath) WHERE p.id IN [22,23] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:20}), (m:Misconception {id:20}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
MATCH (p:ThinkingPath {id:22}), (m:Misconception {id:21}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 生物 ----------
CREATE (c30:Concept {id: 30, name: '细胞呼吸', subject: '生物', grade: '高一', keywords: '有氧呼吸,无氧呼吸,ATP', description: '有机物在细胞内氧化分解释放能量的过程', x: 940, y: 560});
CREATE (c31:Concept {id: 31, name: '遗传规律', subject: '生物', grade: '高二', keywords: '分离定律,自由组合,测交', description: '孟德尔遗传因子的传递规律', x: 1140, y: 640});
CREATE (q30:QuestionType {id: 30, name: '细胞呼吸过程辨析', difficulty: '中', subject: '生物', typicalExample: '有氧呼吸三个阶段的场所与产物'});
CREATE (q31:QuestionType {id: 31, name: '基因分离定律计算', difficulty: '易', subject: '生物', typicalExample: 'Aa 自交后代基因型比例'});
CREATE (p30:ThinkingPath {id: 30, stepOrder: 1, description: '写出亲本基因型', hint: '显性用大写字母表示'});
CREATE (p31:ThinkingPath {id: 31, stepOrder: 2, description: '列出配子并画棋盘格', hint: '雌雄配子随机结合'});
CREATE (p32:ThinkingPath {id: 32, stepOrder: 3, description: '统计表现型与基因型比例', hint: '合并相同格子'});
CREATE (m30:Misconception {id: 30, description: '有氧无氧呼吸场所混淆', frequency: 38});
MATCH (c:Concept {name:'遗传规律'}), (b:Concept {name:'细胞呼吸'}) CREATE (c)-[:PREREQUISITE]->(b);
MATCH (c:Concept {name:'细胞呼吸'}), (q:QuestionType {id:30}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (c:Concept {name:'遗传规律'}), (q:QuestionType {id:31}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:31}), (p:ThinkingPath) WHERE p.id IN [30,31,32] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:30}), (m:Misconception {id:30}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 语文 ----------
CREATE (c40:Concept {id: 40, name: '文言文阅读', subject: '语文', grade: '高中', keywords: '实词,虚词,特殊句式,翻译', description: '课内外文言文的阅读理解与翻译', x: 200, y: 520});
CREATE (q40:QuestionType {id: 40, name: '文言实词辨析', difficulty: '中', subject: '语文', typicalExample: '解释"沛公军霸上"中"军"的用法'});
CREATE (p40:ThinkingPath {id: 40, stepOrder: 1, description: '回归原文语境推断词义', hint: '词不离句，句不离篇'});
CREATE (p41:ThinkingPath {id: 41, stepOrder: 2, description: '联系课内学过的义项', hint: '举一反三迁移'});
CREATE (m40:Misconception {id: 40, description: '以今义解古义（古今异义误判）', frequency: 46});
MATCH (c:Concept {name:'文言文阅读'}), (q:QuestionType {id:40}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:40}), (p:ThinkingPath) WHERE p.id IN [40,41] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:40}), (m:Misconception {id:40}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 英语 ----------
CREATE (c50:Concept {id: 50, name: '定语从句', subject: '英语', grade: '高二', keywords: '关系代词,关系副词,先行词', description: '修饰名词或代词的从句', x: 200, y: 660});
CREATE (q50:QuestionType {id: 50, name: '关系词选择', difficulty: '易', subject: '英语', typicalExample: 'This is the book ______ I bought yesterday.'});
CREATE (p50:ThinkingPath {id: 50, stepOrder: 1, description: '找出先行词并判断指人还是指物', hint: '先行词在从句中充当什么成分？'});
CREATE (p51:ThinkingPath {id: 51, stepOrder: 2, description: '判断从句中缺少主语、宾语还是状语', hint: '缺成分用关系代词，不缺用关系副词'});
CREATE (m50:Misconception {id: 50, description: 'that 与 which 使用场景混淆', frequency: 33});
MATCH (c:Concept {name:'定语从句'}), (q:QuestionType {id:50}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:50}), (p:ThinkingPath) WHERE p.id IN [50,51] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:50}), (m:Misconception {id:50}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 历史 ----------
CREATE (c60:Concept {id: 60, name: '辛亥革命', subject: '历史', grade: '高一', keywords: '武昌起义,中华民国,三民主义', description: '1911 年爆发的资产阶级民主革命', x: 400, y: 560});
CREATE (q60:QuestionType {id: 60, name: '历史意义评析', difficulty: '中', subject: '历史', typicalExample: '简述辛亥革命的历史意义'});
CREATE (p60:ThinkingPath {id: 60, stepOrder: 1, description: '从政治、经济、思想三个维度组织答案', hint: '分点作答'});
CREATE (m60:Misconception {id: 60, description: '只答意义忽略局限性', frequency: 29});
MATCH (c:Concept {name:'辛亥革命'}), (q:QuestionType {id:60}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:60}), (p:ThinkingPath) WHERE p.id IN [60] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:60}), (m:Misconception {id:60}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 地理 ----------
CREATE (c70:Concept {id: 70, name: '大气环流', subject: '地理', grade: '高一', keywords: '气压带,风带,三圈环流', description: '全球范围内大规模的大气运动', x: 600, y: 560});
CREATE (q70:QuestionType {id: 70, name: '气压带风带判读', difficulty: '易', subject: '地理', typicalExample: '北半球中纬西风带的风向'});
CREATE (p70:ThinkingPath {id: 70, stepOrder: 1, description: '确定纬度位置对应的环流圈', hint: '0-30 低纬，30-60 中纬'});
CREATE (p71:ThinkingPath {id: 71, stepOrder: 2, description: '结合地转偏向力判断风向', hint: '北半球向右偏'});
CREATE (m70:Misconception {id: 70, description: '风向命名（来向 vs 去向）混淆', frequency: 35});
MATCH (c:Concept {name:'大气环流'}), (q:QuestionType {id:70}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:70}), (p:ThinkingPath) WHERE p.id IN [70,71] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:70}), (m:Misconception {id:70}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);

// ---------- 政治 ----------
CREATE (c80:Concept {id: 80, name: '市场经济', subject: '政治', grade: '高一', keywords: '市场调节,宏观调控,资源配置', description: '市场在资源配置中起决定性作用的经济体制', x: 780, y: 560});
CREATE (q80:QuestionType {id: 80, name: '市场调节弊端辨析', difficulty: '易', subject: '政治', typicalExample: '市场调节的弊端有哪些'});
CREATE (p80:ThinkingPath {id: 80, stepOrder: 1, description: '回忆自发性、盲目性、滞后性的含义', hint: '结合实例区分三者'});
CREATE (m80:Misconception {id: 80, description: '三大弊端相互混淆', frequency: 31});
MATCH (c:Concept {name:'市场经济'}), (q:QuestionType {id:80}) CREATE (c)-[:HAS_QUESTION_TYPE]->(q);
MATCH (q:QuestionType {id:80}), (p:ThinkingPath) WHERE p.id IN [80] CREATE (q)-[:REQUIRES_PATH {order: p.stepOrder}]->(p);
MATCH (p:ThinkingPath {id:80}), (m:Misconception {id:80}) CREATE (p)-[:COMMON_MISCONCEPTION]->(m);
