@echo off
chcp 65001 >nul
title 思维赋能教学辅助系统 - 一键启动
set "JAVA_HOME=E:\Java\Java 17"
set "PATH=E:\Java\Java 17\bin;%PATH%"
set "NEO4J_BIN=E:\neo4j\neo4j-community-5.26.9\neo4j-community-5.26.9\bin"
cd /d "%~dp0"

REM ============================================================
REM  说明：数据库只存在于 MySQL(mind_empower) 和 Neo4j 中，
REM        项目文件夹里不再存放任何 .sql / .cypher 脚本文件。
REM        如需管理数据，请使用独立的后台管理系统：
REM            http://localhost:7200  （管理员 admin / 123456）
REM        本脚本只做「启动服务」，不做任何重置导入。
REM ============================================================

echo [1/6] 检查 Neo4j ...
netstat -ano | findstr ":7687 " | findstr "LISTENING" >nul
if errorlevel 1 (
    echo     Neo4j 未运行，正在启动并等待 15 秒...
    start "Neo4j" /min "%NEO4J_BIN%\neo4j.bat" console
    timeout /t 15 >nul
) else (
    echo     Neo4j 运行中，跳过启动
)

echo [2/6] 启动 Python AI 服务 (8000) ...
start "AI-Service-8000" cmd /k "cd /d "%~dp0ai-service" && venv\Scripts\python.exe -m uvicorn main:app --port 8000"

echo [3/6] 启动 Java 后端 (8080) ...
start "Java-Backend-8080" cmd /k "cd /d "%~dp0backend" && java -jar mind-server\target\mind-server-1.0.0.jar"

echo [4/6] 启动 Web 前端 (5173) ...
start "Frontend-5173" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo [5/6] 启动移动端 APP 预览 (7100) ...
start "Mobile-App-7100" cmd /k "cd /d "%~dp0mobile-app" && npm run dev"

echo [6/6] 启动后台管理系统 (7200) ...
start "Admin-Console-7200" cmd /k "cd /d "%~dp0admin-console" && npm run dev"

echo.
echo 六个服务已打开，等待 Java 后端就绪（约 15-20 秒）:
echo     Web 教学系统:   http://localhost:5173
echo     移动端 APP:     http://localhost:7100
echo     后台管理系统:   http://localhost:7200
echo.
echo 演示账号: teacher / 123456 ^(教师^)    2023001-2023010 / 123456 ^(学生^)    admin / 123456 ^(后台^)
echo.
echo 关闭六个窗口或用 stop-all.bat 即可停止全部服务
pause
