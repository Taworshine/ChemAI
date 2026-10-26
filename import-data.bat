@echo off
setlocal EnableDelayedExpansion
title 导入知识图谱数据到 Neo4j
cd /d "%~dp0"

echo ============================================
echo   思维赋能系统 - Neo4j 数据导入
echo ============================================
echo.

REM ============================================================
REM  种子脚本统一放在 seeds 文件夹里，按文件名字母顺序执行。
REM  文件名前的数字编号控制顺序：
REM    00_reset.cypher    清空本系统旧的图谱数据
REM    01_init.cypher     化学基础图谱
REM    02_seed_v2.cypher  多学科扩展图谱
REM  想加新数据？直接在 seeds 里放一个 03_xxx.cypher 即可，
REM  无需改动本脚本。
REM ============================================================
if not exist "%~dp0seeds" (
    echo [提示] 未找到 seeds 文件夹，已自动创建。
    echo        请把 .cypher 种子脚本放进去后再运行。
    mkdir "%~dp0seeds"
    goto END
)

set /a TOTAL=0
for %%F in ("%~dp0seeds\*.cypher") do set /a TOTAL+=1
if !TOTAL! EQU 0 (
    echo [提示] seeds 文件夹里没有 .cypher 文件，跳过导入。
    goto VERIFY
)

set /a STEP=0
for %%F in ("%~dp0seeds\*.cypher") do (
    set /a STEP+=1
    echo [!STEP!/!TOTAL!] 导入 %%~nxF ...
    call "%~dp0cypher.bat" "%%~fF" >nul 2>&1
    echo       完成。
)

:VERIFY
echo.
echo 校验导入结果：
echo.
chcp 65001 >nul
call "%~dp0cypher.bat" "%~dp0check_data.cypher" 2>nul | findstr /v "JAVA_TOOL_OPTIONS"
chcp 936 >nul

:END
echo.
if "%~1"=="nopause" exit /b 0
pause
