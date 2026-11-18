# 移动端教研会议系统设计方案

## 一、项目定位

基于现有「思维赋能」教学系统（Spring Boot + Vue3 + MySQL + Neo4j + Python AI），开发配套移动 APP，核心场景为**高中教研组开会**（教案评审、集体备课、命题研讨），附加学情查看与资源管理功能。

**技术栈**：HBuilder X + uni-app (Vue3) + uView UI + Spring Boot + WebSocket + MySQL + Neo4j

---

## 二、核心功能清单

### 2.1 会议管理
- 创建会议：选学科（化学/数学/物理…）、设议程（教案评审/集体备课/命题研讨/试卷分析）、选参会教师（复用 sys_user）
- 生成 6 位会议号 + 二维码，扫码/输码加入
- 会议列表：我主持的 / 我参与的 / 历史会议
- 主持人控制：开始/结束、切换环节、锁定会议、移出成员

### 2.2 教案评审（核心）
- 教案上传：拍照上传纸质教案，或选已有电子教案（复用 `/api/document/upload`）
- 教案预览：PDF 在线查看、缩放
- 实时批注：
  - 手写圈画（Canvas，颜色/粗细可选）
  - 文字批注、60 秒语音批注
  - 批注回复、标记解决、实时同步给所有人
- 量化评审（与 Neo4j 图谱联动）：
  - 自动分析：上传后调现有 AI 解析提取知识点，与图谱比对，给出
    - 知识覆盖率（覆盖必备 Concept 百分比）
    - 思维路径完整性（是否包含完整 ThinkingPath）
    - 重难点预判（是否覆盖 Misconception 常见错误）
  - 手动评分：1-5 星，维度可自定义
  - 自动汇总评审表，保留历次评审历史

### 2.3 实时互动
- 即时聊天：文字/表情/图片/文件、@提醒、消息引用
- 语音通话（可选）、视频会议（可选）
- 实时投票：
  - 单选/多选/评分投票，实时显示结果
  - 匿名/实名切换、倒计时自动结束
  - 结果图表导出

### 2.4 命题研讨
- 题目录入：拍照 OCR 识别，自动关联知识点
- 题目查重：检测是否考查同一 Neo4j 概念节点
- 组卷：手动选题 / 智能组卷（按知识点分布+难度比例自动生成）
- 审题流程：提交 → 组内批注修改 → 投票通过（2/3 以上）→ 入 question_bank

### 2.5 会议纪要与任务
- 自动记录：聊天记录、批注汇总、投票结果、决议消息
- 纪要生成：AI/规则自动生成结构化纪要（背景/讨论/决议/任务），主持人可编辑、确认、分发
- 任务跟踪：提取待办 → 分配负责人与截止时间 → 到期提醒 → 完成回写 Web 端

### 2.6 资源中心
- 教案库：高分教案自动入库、收藏、按学科（Neo4j 学科节点）分类
- 试题库：组内共享题、真题、错题本（拍照收录自动关联知识点）
- 资料库：课件/视频/文档，标签管理与搜索

### 2.7 学情分析（轻量）
- 班级平均分、知识点掌握度热力图（复用 `/api/analysis/class/heatmap`）
- 高频错题 Top10、知识点错误分布、学生个体图谱
- AI 教学建议、薄弱点教案推荐

### 2.8 个人中心
- 我的会议/教案/任务、消息通知（会议提醒/批注回复/任务提醒）
- 设置：改密码、通知开关、清缓存

---

## 三、技术架构

```
uni-app 客户端（教师端/组长端，按角色切换）
        │ HTTPS + satoken
Spring Boot（现有 8080，新增 mobile 包）
        │
  MySQL（新增 7 张表） + Neo4j（复用图谱） + Python AI（复用解析/对话）
```

### 3.1 前端
| 技术 | 用途 |
|---|---|
| HBuilder X + uni-app (Vue3) | 跨平台开发打包 APK |
| uView UI | 移动端组件 |
| Canvas | 手写批注（相对坐标 x,y 存百分比，防屏幕错位） |
| WebSocket / 3 秒轮询 | 批注与投票实时同步（MVP 用轮询） |
| uCharts | 投票/评审图表 |
| uni.uploadFile + compressImage | 教案/图片上传（压缩后传） |

### 3.2 后端
- 现有 Spring Boot 新增 `mobile` 包，复用现有 Service，不动老代码
- 实时通信 MVP 用轮询，进阶加 `spring-boot-starter-websocket`
- sa-token 复用登录态；CORS 放行 `/api/mobile/**`

### 3.3 数据库新增表
```sql
mobile_meeting        -- 会议（meeting_no 6位号、agenda、status、current_doc_id）
mobile_meeting_member -- 成员（HOST/MEMBER、在线状态）
mobile_annotation     -- 批注（DRAW/TEXT/VOICE、draw_data JSON、相对坐标 x/y、reply_to、is_resolved）
mobile_vote           -- 投票（SINGLE/MULTIPLE/SCORE、options JSON、匿名、结束时间）
mobile_vote_record    -- 投票记录（option_id / score）
mobile_summary        -- 纪要（content、auto_generate、is_confirmed）
mobile_task           -- 任务（assignee_id、deadline、status，回写 Web 端）
```

---

## 四、API 设计（新增）

```
会议  POST /api/mobile/meeting/create | POST /meeting/{id}/join | GET /meeting/{id}/status
      POST /meeting/{id}/close | GET /meeting/{id}/members | GET /meeting/list
批注  POST /annotation/add | GET /annotation/list | DELETE /annotation/{id}
      POST /annotation/{id}/reply | POST /annotation/{id}/resolve
投票  POST /vote/create | POST /vote/submit | GET /vote/{id}/result | POST /vote/{id}/close
纪要  POST /summary/generate | GET /summary/{id} | POST /summary/{id}/confirm
任务  GET /task/list | POST /task/{id}/complete
命题  POST /question/duplicate-check | POST /paper/generate | GET /paper/{id}/export
```

**复用现有接口**：`/api/auth/login`、`/api/document/upload`、`/api/document/parse/{id}`、`/api/graph/full`、`/api/analysis/class/heatmap`、`/api/agent/chat`

---

## 五、关键实现点

1. **批注坐标**：全部用相对坐标（百分比）存储与回放，避免不同手机屏幕错位
2. **实时同步**：MVP 用 3 秒轮询拉取批注+投票状态，人少稳定；后续升级 WebSocket
3. **教案-图谱联动**：上传 → 现有 AI 提取知识点 → 与 Neo4j 标准 Concept/ThinkingPath 比对 → 自动算覆盖率写进评审表
4. **离线支持**：会议资料进会前缓存本地；批注断网存 SQLite，恢复网络自动同步
5. **数据回写**：会议结束 → 教案标已评审、通过题入 question_bank、任务同步 Web 端
6. **部署**：默认局域网直连学校服务器（APP 首次启动可配服务器 IP），可选云端 HTTPS 部署

---

## 六、开发路线

- **第 1-2 周 MVP**：会议创建/加入、教案上传预览、文字+手绘批注、即时聊天
- **第 3-4 周**：量化评审、实时投票、纪要生成、任务跟踪
- **第 5-6 周**：命题研讨、资源中心、学情分析、离线与打包优化
