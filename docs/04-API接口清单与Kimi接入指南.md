# 04 · API 接口清单与 Kimi 接入指南

> 本文档两部分：①全量 REST API 清单（供后期联调 / 写新前端 / 接 Kimi 时对照）；
> ②把 AI 服务从 DeepSeek 切换到 Kimi（Moonshot）的完整步骤。

---

## 一、通用约定

| 项 | 约定 |
|---|---|
| 网关 | 前端经 Vite 代理访问 `/api/**` → Java 后端 `:8080`；AI 服务 `:8000` 只被 Java 后端内部调用 |
| 认证 | 除 `POST /api/auth/login` 外，全部接口需请求头 `satoken: <登录返回的 token>` |
| 响应包装 | 统一 `Result<T>`：`{"code": 200, "msg": "success", "data": ...}`；`code != 200` 即失败 |
| 角色 | `TEACHER` 教师（teacher/123456）、`STUDENT` 学生（2023001~2023010/123456） |

登录示例：

```bash
curl -X POST http://localhost:5173/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher","password":"123456"}'
# → {"code":200,"data":{"token":"...","role":"TEACHER","realName":"...","userId":1}}
```

---

## 二、Java 后端 API 清单（:8080，前缀 /api）

### 认证（mind-system）
| 方法 | 路径 | 说明 | 请求体 |
|---|---|---|---|
| POST | `/auth/login` | 登录，返回 token/role/userId | `{"username","password"}`（密码 SHA-256 比对） |
| GET | `/auth/info` | 当前用户信息 | - |
| POST | `/auth/logout` | 退出登录 | - |

### 知识图谱（mind-knowledge，数据在 Neo4j）
| 方法 | 路径 | 说明 | 参数 |
|---|---|---|---|
| GET | `/graph/full` | 全量图（两页面初始化用，前端再做学科/关键词/展开过滤） | - |
| GET | `/graph/subjects` | 图谱中出现的学科列表 | - |
| GET | `/graph/nodes` | 节点分页搜索（后台管理页用） | `label`(Concept/QuestionType/ThinkingPath/Misconception，默认 Concept)、`subject`、`keyword`、`page`、`size` |
| POST | `/graph/node` | 新建节点 | `{"label": "Concept", "props": {...}}` |
| PUT | `/graph/node/{elementId}` | 更新节点属性（含拖拽后的 x/y 坐标） | props 键值对 |
| DELETE | `/graph/node/{elementId}` | 删除节点及其全部关系 | - |
| POST | `/graph/edge` | 新建关系 | `{"source", "target", "type"}`，type ∈ HAS_QUESTION_TYPE / REQUIRES_PATH / COMMON_MISCONCEPTION / PREREQUISITE |
| DELETE | `/graph/edge/{elementId}` | 删除关系 | - |

`/graph/full` 返回结构：`data.nodes = [{id, label(类型), props(全部属性含 x/y/subject)}]`，`data.edges = [{id, source, target, type}]`。

### 题库（mind-knowledge，数据在 MySQL）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/question/list?subject=&keyword=&page=&size=` | 分页题库列表 |
| POST | `/question` | 新增题目 `{"subject","conceptName","questionType","content","answer","difficulty"}` |
| PUT | `/question/{id}` | 修改题目 |
| DELETE | `/question/{id}` | 删除题目 |

### 教案文档（mind-knowledge）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/document/upload` | multipart 上传 PDF/Word（字段名 `file`） |
| GET | `/document/list` | 文档列表（含解析状态） |
| POST | `/document/parse/{id}` | 触发解析：Java 提取文本 → 调 AI 服务 `/api/extract` 抽概念/题型/思维路径/卡点 |

### AI 对话追问（mind-agent，SSE 流式）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/agent/session` | 创建会话（可选 `{"questionTypeId": n}` 绑定题型，上下文会自动带上该题型的思维路径） |
| GET | `/agent/sessions` | 当前学生会话列表 |
| GET | `/agent/history/{id}` | 会话历史消息 |
| PUT | `/agent/session/{id}/status` | 关闭/标记解决 `{"status": 1}` |
| POST | `/agent/chat` | **SSE**，`{"sessionId", "message"}`，流式返回 AI 苏格拉底式追问；消息带 `misconception_type`（卡点诊断）与 `next_step`（思维步推进） |

### 在线练习与学情分析（mind-analysis，MySQL）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/practice/questions?subject=化学&count=5` | 随机抽题（不返回答案） |
| POST | `/practice/submit` | 提交作答，自动判分归档 `{"subject","answers":[{"questionId","userAnswer"}]}` |
| GET | `/practice/records` | 我的练习记录（学生） |
| GET | `/practice/stats` | 练习统计（学生端报告页） |
| GET | `/analysis/students` | 学生列表 + 练习概览（教师） |
| GET | `/analysis/class/heatmap` | 班级概念掌握度热力图（教师看板） |
| GET | `/analysis/student/{id}/radar` | 个体五维思维雷达图 + 概念掌握明细 |

---

## 三、Python AI 服务 API（:8000，仅内部调用）

由 Java 后端的 `mind-common/AiServiceClient` 发起，一般不直接暴露给前端。

| 方法 | 路径 | 请求体 | 返回 |
|---|---|---|---|
| POST | `/api/chat` | `{"message", "history":[{role,content}], "context":{path:[],current_step,misconception}}` | `{"reply", "misconception_type", "next_step", "engine", "ok"}` |
| POST | `/api/extract` | `{"doc_id", "file_path"?, "text"?}` | `{"concepts", "question_types", "thinking_paths":[{name,steps}], "misconceptions", "engine", "ok"}` |

`engine` 字段标识实际使用的引擎：`deepseek` / `kimi`（配了 Key 时）或 `rule-based`（无 Key 自动降级，内置规则引擎，功能可完整演示）。

---

## 四、切换到 Kimi API 的步骤

AI 服务的 LLM 封装是**供应商无关**的（OpenAI SDK + 环境变量预设），切 Kimi 只需配置，无需改代码。

### 方式 A：`.env` 文件（推荐）

在 `E:\agent+neo4j\ai-service\` 新建 `.env`：

```ini
LLM_PROVIDER=kimi
KIMI_API_KEY=sk-你的KimiKey
# 可选：覆盖默认模型（默认 kimi-k2-0905-preview，你的账号需有该模型权限）
LLM_MODEL=kimi-k2.6
```

服务启动时会自动加载 `.env`（`services/llm.py` 里已内置 python-dotenv，且 `.env` 优先于系统环境变量）。

> 本机实际配置（2026-10-22 验证通过）：该开放平台账号可用模型为 `kimi-k2.6` / `kimi-k2.7-code`，
> 已配置 `LLM_MODEL=kimi-k2.6`（k2.6 为推理模型，temperature 固定为 1、回复前先输出思维链；
> 免费档组织限流 3 RPM，`llm.py` 已内置限流退避重试，超限自动降级规则引擎）。

### 方式 B：系统环境变量

```bat
setx LLM_PROVIDER kimi
setx KIMI_API_KEY sk-你的KimiKey
```

### 生效与验证

1. **重启 AI 服务**（环境变量在启动时读取）：

   ```bat
   :: 停掉 8000 端口进程后（或关窗），重新执行 start-all.bat 的第 4 步：
   cd E:\agent+neo4j\ai-service
   venv\Scripts\python.exe -m uvicorn main:app --port 8000
   ```

2. **验证引擎已切换**：

   ```bash
   curl -X POST http://localhost:8000/api/chat -H "Content-Type: application/json" \
     -d "{\"message\":\"怎么配平 Cu+HNO3\",\"context\":{\"path\":[\"标化合价\",\"电子守恒\",\"配系数\"]}}"
   # engine 应为 "kimi"；若 Key 无效/欠费，自动降级 rule-based，engine 相应变化
   ```

3. 前端无感知：学生端追问、教案解析走的都是同一个 `/api/chat`、`/api/extract`。

### 关键参数对照表

| 供应商 | Key 环境变量 | 默认 base_url | 默认模型 |
|---|---|---|---|
| DeepSeek（默认） | `DEEPSEEK_API_KEY` | `https://api.deepseek.com` | `deepseek-chat` |
| Kimi（Moonshot） | `KIMI_API_KEY` | `https://api.moonshot.cn/v1` | `kimi-k2-0905-preview` |
| 任意 OpenAI 兼容服务 | `LLM_API_KEY` + `LLM_BASE_URL` + `LLM_MODEL` | 自定义 | 自定义 |

- Key 查找优先级：供应商专属变量 > `LLM_API_KEY` > 旧 `DEEPSEEK_API_KEY`；`.env` 文件优先于系统环境变量。
- **降级机制**：任何一次 LLM 调用失败（网络、欠费、限流）都会静默落到内置规则引擎，页面不会报错；排障时先看返回里的 `engine` 字段（`kimi` = 真实模型，`rule-based (...)` = 已降级及原因）。
- Kimi 官方接口为 OpenAI 兼容的 `chat/completions`，K2 系列模型中文教学场景表现与 DeepSeek 相当，直接替换即可。
- 如需切回 DeepSeek：删掉 `.env` 里的两行或改 `LLM_PROVIDER=deepseek`，重启服务。
