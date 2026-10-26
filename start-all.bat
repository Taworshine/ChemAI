@echo off
title 思维赋能教学辅助系统 - 一键启动
set "JAVA_HOME=E:\Java\Java 17"
set "PATH=E:\Java\Java 17\bin;%PATH%"
set "NEO4J_BIN=E:\neo4j\neo4j-community-5.26.9\neo4j-community-5.26.9\bin"
cd /d "%~dp0"

REM ========== 先导入数据，再启动服务 ==========

echo [1/6] 检查 Neo4j ...
netstat -ano | findstr ":7687 " | findstr "LISTENING" >nul
if errorlevel 1 (
    echo     Neo4j 未运行，正在启动，请等待 15 秒...
    start "Neo4j" /min "%NEO4J_BIN%\neo4j.bat" console
    timeout /t 15 >nul
) else (
    echo     Neo4j 已在运行，跳过。
)

echo [2/6] 导入 MySQL 数据 ...
call "%~dp0init-sql.bat" nopause

echo [3/6] 导入 Neo4j 图谱数据 ...
call "%~dp0import-data.bat" nopause

echo [4/6] 启动 Python AI 服务 (8000) ...
start "AI-Service-8000" cmd /k "cd /d "%~dp0ai-service" && venv\Scripts\python.exe -m uvicorn main:app --port 8000"

echo [5/6] 启动 Java 后端 (8080) ...
start "Java-Backend-8080" cmd /k "cd /d "%~dp0backend" && java -jar mind-server\target\mind-server-1.0.0.jar"

echo [6/6] 启动前端 (5173) ...
start "Frontend-5173" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo 四个窗口已打开，等 Java 后端启动完成（约 15-20 秒）后访问:
echo     系统入口: http://localhost:5173
echo     演示账号: teacher / 123456 ^(教师^)    2023001-2023010 / 123456 ^(学生^)
echo.
echo 关闭那四个窗口即可停止全部服务。
pause
