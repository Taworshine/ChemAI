# 移动端教研会议 APP 使用说明

对应《移动端教研会议APP设计方案》。APP 供高中教研组开会使用：教案评审、集体备课、命题研讨，附学情查看与任务跟踪。

## 一、工程结构

```
mobile-app\                    uni-app (Vue3 + Vite) 前端，可用 HBuilderX 打开打包 APK
├── src\pages\login            登录（复用 /api/auth/login，satoken 请求头）
├── src\pages\meeting\list     会议列表（我主持的/我参与的/历史）
├── src\pages\meeting\create   创建会议（学科/议程/参会教师 → 生成 6 位会议号）
├── src\pages\meeting\join     输 6 位会议号加入
├── src\pages\meeting\room     会议房间（教案/批注/讨论/投票/成员 五个页签）
├── src\pages\review\analyze   量化评审（知识覆盖率/思维路径完整性/重难点预判）
├── src\pages\analysis         班级学情热力图（复用 /api/analysis/class/heatmap）
├── src\pages\mine             我的（任务、服务器设置、退出）
└── src\utils\request.js       请求封装（自动带 satoken；APP 端首次启动可配服务器 IP）

backend\mind-mobile            后端新增 Maven 模块（com.mind.mobile，不动老代码）
sql\05_mobile_mysql.sql        新增 8 张表（7 张方案表 + mobile_chat_message）+ 种子会议
```

## 二、部署步骤（学校局域网）

1. **数据库**：`cmd /c init-sql.bat`（自动按顺序执行 sql\ 全部脚本，含 05_mobile_mysql.sql）
2. **后端**：`cd backend && mvn -DskipTests clean package`，跑 `mind-server\target\mind-server-1.0.0.jar`（已含 /api/mobile/** 全部接口与 CORS 放行）
3. **前端 H5 预览**：`cd mobile-app && npm install && npm run dev:h5`（端口 7100，代理到 8080）
4. **打包 APK**：用 HBuilderX「文件→打开目录」选择 `mobile-app`，
   - 首次在 manifest.json 里填自己的 uni-app appid（免费申请）
   - 菜单「发行→原生App-云打包」，选 Android（使用公共测试证书即可），产出 apk
   - APP 首次启动在登录页点"服务器地址"，填 `http://<学校服务器IP>:8080`

## 三、已实现功能对照

| 方案功能 | 状态 |
|---|---|
| 创建会议（学科/议程/参会教师）、6 位会议号、输码加入 | ✅ |
| 会议列表（我主持的/我参与的/历史） | ✅ |
| 主持人控制：开始/结束/锁定/切换教案/移出成员 | ✅ |
| 教案上传（拍照/选图，复用 /api/document/upload）+ AI 解析 + 在线预览 | ✅（图片直接预览；PDF 经 /api/mobile/document/{id}/file 下载查看） |
| 手绘圈画批注（Canvas，相对坐标百分比存储回放，防屏幕错位） | ✅ |
| 文字批注、批注回复、标记解决、实时同步（3 秒轮询） | ✅ |
| 60 秒语音批注 | 后端接口已就绪（/annotation/voice），前端录制入口待加 |
| 即时聊天（文字，引用字段已留） | ✅（图片/文件消息待加） |
| 实时投票：单选/多选/评分、匿名/实名、到期自动结束、实时条形图结果 | ✅ |
| 量化评审：AI 解析结果 × Neo4j 图谱比对（覆盖率/思维路径/重难点） | ✅ |
| 纪要自动生成（规则引擎：背景/讨论/决议/任务）+ 主持人确认 | ✅ |
| 任务跟踪：提取待办、分配负责人/截止、完成回写 | ✅ |
| 命题研讨：题目查重（同概念检测）、智能组卷（按知识点分布）、试卷导出 | ✅（拍照 OCR 待接 OCR 服务） |
| 班级学情热力图 | ✅ |
| 资源中心（教案库/试题库/资料库） | 待开发（复用 edu_document/question_bank） |
| WebSocket 实时推送 | 当前为 3 秒轮询（方案 MVP 约定），后续升级 |
| 离线缓存（SQLite 断网续传） | 待开发 |

## 四、演示路径

1. `start-all.bat` 起全套服务后，H5 打开 7100 或安装 APK
2. 登录 `teacher / 123456` → 会议列表里已有种子会议"高三化学教研组：氧化还原教案评审"（会议号 888888，含 3 条讨论、2 条批注、1 个投票、1 个任务）
3. 进入会议：教案页签拍照上传一张教案照片 → 主持人点"AI 解析" → "量化评审"看图谱覆盖率
4. 手绘批注提交后切到批注页签可见；投票提交后实时显示条形图；我的页可完成任务
