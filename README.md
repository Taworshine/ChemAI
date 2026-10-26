# 思维赋能 · 高中化学教学辅助系统（本地开发版）

基于大模型的"AI 思维引导者"教学辅助系统：AI 通过启发式追问引导学生自主解题，而非直接给答案。含教师端（知识图谱编辑、文档管理、学情看板）与学生端（智能追问、学情报告）。

## 技术栈

| 端 | 技术 |
|---|---|
| Java 后端 (8080) | Java 17 + Spring Boot 3.2 + Maven 多模块 + MyBatis-Plus + Sa-Token + Neo4j Java Driver |
| Python AI 服务 (8000) | Python 3.12+ + FastAPI + pdfplumber / python-docx + DeepSeek API |
| 数据库 | MySQL 8.0（业务数据，库名 `mind_empower`）、Neo4j 5.26 Community（知识图谱） |
| 前端 (5173) | Vue 3 + Vite + Element Plus + AntV G6 + ECharts 5 |

```
本地环境
├── Java Backend (Spring Boot, :8080)  ──HTTP──▶  Python AI Service (FastAPI, :8000)
│   ├── MyBatis-Plus ──▶ MySQL :3306              └── DeepSeek API（无 Key 自动降级规则引擎）
│   └── Neo4j Driver ──▶ Neo4j :7687
└── Vue3 Frontend (Vite, :5173)  ──/api 代理──▶ Java Backend
```

## 一、首次启动（已在本机完成）

```bash
# 1. MySQL：建库建表 + 种子数据（密码 root/psxngS4，sql 文件夹按数字前缀顺序执行）
#    懒人做法：直接 cmd /c init-sql.bat，自动按顺序执行下面全部脚本

"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -ppsxngS4 --default-character-set=utf8mb4 < sql\01_schema.sql
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -ppsxngS4 --default-character-set=utf8mb4 < sql\02_schema_v2.sql
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -ppsxngS4 --default-character-set=utf8mb4 < sql\03_seed_mysql.sql
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -ppsxngS4 --default-character-set=utf8mb4 < sql\04_seed_v2_mysql.sql

# 2. Neo4j：约束 + 图谱种子数据（密码 neo4j/ppsxngS4）
#    import-data.bat 会自动扫描 seeds 文件夹里所有 .cypher 并按文件名顺序导入
cmd /c import-data.bat

# 3. Python 依赖（已装好，虚拟环境在 ai-service\venv）
cd ai-service && venv\Scripts\python.exe -m pip install -r requirements.txt

# 4. 后端编译（产物 backend\mind-server\target\mind-server-1.0.0.jar）
cd ..\backend && mvn -DskipTests clean package

# 5. 前端依赖
cd ..\frontend && npm install
```

## 二、日常启动

**方式 A（推荐）**：双击项目根目录的 `start-all.bat`，按 6 步自动完成：检查/启动 Neo4j → 导入 MySQL 数据（init-sql.bat）→ 导入 Neo4j 图谱（import-data.bat）→ 启动 AI 服务 + Java 后端 + 前端，共 4 个窗口。

> ⚠️ 启动即重置：`init-sql.bat` 和 `import-data.bat` 都是**幂等重置式导入**——每次启动都会先把 MySQL 和 Neo4j 恢复为种子数据，练习记录、聊天记录等使用数据会被清空。如需保留使用数据，启动后不要再手动运行这两个脚本。

**方式 B（手动，4 个终端）**：

```bash
# 终端 1：Neo4j（如已在运行可跳过）
set "JAVA_HOME=E:\Java\Java 17"
"E:\neo4j\neo4j-community-5.26.9\neo4j-community-5.26.9\bin\neo4j.bat" console

# 终端 2：Python AI 服务
cd E:\agent+neo4j\ai-service
venv\Scripts\python.exe -m uvicorn main:app --port 8000

# 终端 3：Java 后端
cd E:\agent+neo4j\backend
set "PATH=E:\Java\Java 17\bin;%PATH%"
java -jar mind-server\target\mind-server-1.0.0.jar

# 终端 4：前端
cd E:\agent+neo4j\frontend
npm run dev
```

访问 **http://localhost:5173**　｜　Neo4j 浏览器：**http://localhost:7474**

演示账号：`teacher / 123456`（教师）、`2023001` ~ `2023010 / 123456`（学生，高三(2)班）

## 三、接入大模型（可选：DeepSeek / Kimi）

不配 Key 系统也能完整演示（自动使用内置苏格拉底规则引擎）。配置真实模型：在 `ai-service` 目录新建 `.env`：

```ini
# DeepSeek（默认）
DEEPSEEK_API_KEY=sk-你的key

# 或 Kimi（Moonshot）
LLM_PROVIDER=kimi
KIMI_API_KEY=sk-你的KimiKey
LLM_MODEL=kimi-k2.6
```

改完重启 AI 服务（8000）生效。详细接口清单与切换步骤见 `docs/04-API接口清单与Kimi接入指南.md`。
本机已配置 Kimi（模型 kimi-k2.6）并验证通过。

## 四、功能与验收对照

| 验收项 | 位置 |
|---|---|
| 上传 PDF/Word 教案 → Python 提取文本 → DeepSeek 抽取概念/题型/思维路径/卡点 | 教师端 → 文档管理，点"开始解析" |
| Neo4j 图谱"概念-题型-思维路径"可视化：初始只显示核心概念，点击节点展开/收起上下游（带 +N 徽标提示隐藏邻居），拖拽自动保存坐标 | 教师端 → 知识图谱 / 学生端 → 知识图谱 |
| 学生提问不直接给答案，而是苏格拉底式追问 | 学生端 → 智能追问，试试输入"Cu+HNO3 怎么配平" |
| 班级概念掌握度热力图 + 个体五维雷达图 | 教师端 → 学情看板 |
| 卡点类型诊断（概念不清/计算错误/逻辑错误）并记录到会话总结 | 追问过程中自动标注，学生端 → 学情报告可见 |

## 五、目录结构

```
E:\agent+neo4j\
├── sql\                            MySQL 脚本（00 整库重置 → 01 建库 → 02 扩展表 → 03/04 种子数据，按文件名顺序执行）
├── seeds\                          Neo4j 种子脚本（00 清空 → 01 化学 → 02 多学科，自动扫描导入）
├── init-sql.bat                    MySQL 一键导入（扫描 sql\ 全部脚本，幂等，可单独运行）
├── import-data.bat / cypher.bat    Neo4j 数据导入（扫描 seeds）/ Cypher UTF-8 执行器
├── start-all.bat / stop-all.bat    一键启动（先导入 MySQL+Neo4j 数据再启动服务）/ 一键停止
├── backend\                        Maven 多模块
│   ├── mind-common                 Result<T>、AI 服务 HTTP 客户端
│   ├── mind-system                 用户登录（Sa-Token）
│   ├── mind-knowledge              文档上传解析、Neo4j 图谱 CRUD
│   ├── mind-agent                  对话会话、SSE 流式追问
│   ├── mind-analysis               学情统计（热力图/雷达）
│   └── mind-server                 启动模块 + application.yml
├── ai-service\                     FastAPI：文档解析、DeepSeek 调用、追问生成
└── frontend\                       Vue3：登录/图谱编辑/文档/看板/对话/报告
```

## 六、V2 功能说明（多学科 + 后台管理）

- **多学科覆盖**：语 / 数 / 英 / 物 / 化 / 生 / 政 / 史 / 地，图谱与题库均带学科属性，全站可筛选
- **知识图谱**：G6 可视化支持学科切换、关键词搜索、节点拖拽自动保存、点击编辑、关系创建（概念→题型→思维路径→卡点）与删除，与 Neo4j 实时读写
- **知识管理后台**：概念 / 题型 / 思维路径 / 卡点四类元数据的分页 CRUD（学科筛选 + 搜索）
- **题库管理**：多学科题目的增删改查，与在线练习联动
- **在线练习（学生端）**：按学科随机抽题 → 作答 → 自动判分 → 错题回顾 → 归档学情报告
- **教师工作台**：学生数 / 图谱节点 / 文档 / 题库 / 练习次数统计 + 学科均分图表 + 最近动态

V2 数据初始化：

```bash
cmd /c init-sql.bat           # 自动扫描 sql\：00 整库重置 → 01 建库 → 02 扩展表 → 03/04 种子数据
cmd /c import-data.bat        # 自动扫描 seeds：00 清空 → 01 化学 → 02 多学科
```

> ⚠️ 注意：执行 cypher 脚本必须用项目根目录的 `cypher.bat`（已处理 UTF-8 编码，直接调用 cypher-shell 会导致中文乱码入库）。

## 七、常用维护命令

```bash
# 重置 Neo4j 图谱数据（在 cypher-shell 或 cypher.bat 中）
MATCH (n) WHERE any(l IN labels(n) WHERE l IN ['Concept','QuestionType','ThinkingPath','Misconception']) DETACH DELETE n;
# 然后重新导入 seeds 文件夹（import-data.bat 会先清空再导入）

# 重置 MySQL 数据：重跑 sql\ 文件夹全部脚本（含 00_reset.sql 整库重建）
cmd /c init-sql.bat
```

> 注意：本机 Java 17 在 `E:\Java\Java 17`（未加入系统 PATH），启动脚本已内置该路径。
