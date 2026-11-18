@echo off
chcp 65001 >nul
title 思维赋能教学辅助系统 - 一键停止
set "JAVA_HOME=E:\Java\Java 17"
set "PATH=E:\Java\Java 17\bin;%PATH%"
cd /d "%~dp0"

echo 正在停止全部服务...
echo.

REM 按端口结束: 7200(后台管理) 7100(移动端) 5173(Web前端) 8080(Java) 8000(AI服务) 7687(Neo4j)
for %%P in (7200 7100 5173 8080 8000 7687) do (
    echo [端口 %%P]
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr ":%%P " ^| findstr "LISTENING"') do (
        taskkill /PID %%A /F >nul 2>&1
        if not errorlevel 1 (echo     已结束进程 PID %%A) else (echo     未找到进程)
    )
)

echo.
echo 全部服务已停止，5 秒后自动关闭...
timeout /t 5 >nul
