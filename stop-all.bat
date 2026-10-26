@echo off
title 思维赋能教学辅助系统 - 一键停止
set "JAVA_HOME=E:\Java\Java 17"
set "PATH=E:\Java\Java 17\bin;%PATH%"
rem 项目使用专用实例 E:\neo4j\mind-empower-neo4j（7688/7475），不触碰 7687 上的其他项目
cd /d "%~dp0"

echo 正在停止全部服务...
echo.

REM 按端口结束: 5173(前端) 8080(Java) 8000(AI服务) 7687(Neo4j)
for %%P in (5173 8080 8000 7687) do (
    echo [端口 %%P]
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr ":%%P " ^| findstr "LISTENING"') do (
        taskkill /PID %%A /F >nul 2>&1
        if not errorlevel 1 (echo     已结束进程 PID %%A) else (echo     未在运行)
    )
)

echo.
echo 已清理完毕，5 秒后自动关闭...
timeout /t 5 >nul
