@echo off
setlocal EnableDelayedExpansion
title 导入 MySQL 数据
cd /d "%~dp0"

set "MYSQL=D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PWD=psxngS4"

echo ============================================
echo   思维赋能系统 - MySQL 数据导入
echo ============================================
echo.

if not exist "%MYSQL%" (
    echo [错误] 未找到 MySQL 客户端: %MYSQL%
    echo        请修改本脚本开头的 MYSQL 路径。
    goto END
)

netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [1/3] MySQL 未运行，正在尝试启动 MySQL80 服务 ...
    net start MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo        启动失败（可能需要以管理员身份运行），请手动启动 MySQL 后重试。
        goto END
    )
    timeout /t 5 >nul
    echo        已启动。
) else (
    echo [1/3] MySQL 已在运行，跳过。
)

echo [2/3] 执行 sql\*.sql ...
set /a TOTAL=0
for %%F in ("%~dp0sql\*.sql") do set /a TOTAL+=1
if !TOTAL! EQU 0 (
    echo        [提示] sql 文件夹里没有 .sql 文件，跳过。
    goto VERIFY
)
set /a STEP=0
for %%F in ("%~dp0sql\*.sql") do (
    set /a STEP+=1
    echo        [!STEP!/!TOTAL!] %%~nxF
    "%MYSQL%" -uroot -p%MYSQL_PWD% --default-character-set=utf8mb4 < "%%~fF" >nul 2>&1
    if errorlevel 1 (echo              ^^! 该脚本执行出错，请检查内容。) else (echo              完成。)
)

:VERIFY
echo [3/3] 校验导入结果：
"%MYSQL%" -uroot -p%MYSQL_PWD% --default-character-set=utf8mb4 -e "USE mind_empower; SELECT (SELECT COUNT(*) FROM sys_user) AS users, (SELECT COUNT(*) FROM question_bank) AS questions, (SELECT COUNT(*) FROM student_profile) AS profiles, (SELECT COUNT(*) FROM dialog_session) AS sessions;" 2>nul

:END
echo.
if "%~1"=="nopause" exit /b 0
pause
