@echo off
REM ============================================================
REM  cypher.bat - UTF-8 safe Cypher executor
REM  Usage: cypher.bat <cypher-file-path>
REM  Fix: cypher-shell on Chinese Windows decodes UTF-8 .cypher
REM       files as GBK by default, causing garbled Chinese data.
REM ============================================================
setlocal EnableExtensions

REM 1) Set console code page to UTF-8
chcp 65001 >nul

REM 2) Force JVM to read files as UTF-8 (the core fix)
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8"

REM 3) Run cypher-shell with the given file
"D:\neo4j-community-5.8.0\bin\cypher-shell.bat" -u neo4j -p ppsxngS4 -f "%~1"

endlocal
