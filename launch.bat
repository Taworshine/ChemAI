@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "ROOT=%~dp0"

echo ============================================
echo   ChemAI 一键启动
echo ============================================

echo [1/3] 启动 AI 服务 (port 8000)...
start "AI Service" cmd /k "cd /d %ROOT%ai-service && venv\Scripts\python.exe -m uvicorn main:app --port 8000"

echo [2/3] 启动 Java 后端 (port 8080)...
start "Java Backend" cmd /k "cd /d %ROOT%backend && java -jar mind-server\target\mind-server-1.0.0.jar"

echo [3/3] 启动前端 (Vite)...
start "Frontend" cmd /k "cd /d %ROOT%frontend && npm run dev"

echo.
echo 三个服务已在独立窗口启动，关闭对应窗口即可停止该服务。
pause
