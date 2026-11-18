@echo off
chcp 65001 >nul
rem =============================================================
rem  db.bat - direct database query entry (query via stdin, no .sql/.cypher files)
rem
rem  Usage:
rem    echo SELECT * FROM sys_user | db.bat mysql
rem    echo MATCH (n:Concept) RETURN n.name | db.bat neo4j
rem
rem  mysql -> mind_empower (utf8mb4)
rem  neo4j -> Neo4j 5.26 (PowerShell wrapper for UTF-8 Chinese)
rem =============================================================
setlocal
set "MYSQL=D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "CYPHER_SHELL=E:\neo4j\neo4j-community-5.26.9\neo4j-community-5.26.9\bin\cypher-shell.bat"

if /i "%~1"=="mysql" goto MYSQL
if /i "%~1"=="neo4j" goto NEO4J
echo [db.bat] usage: echo ^<query^> ^| db.bat mysql^|neo4j
exit /b 1

:MYSQL
"%MYSQL%" -u root -ppsxngS4 --default-character-set=utf8mb4 mind_empower
exit /b 0

:NEO4J
set "NEO4J_HOME=E:\neo4j\neo4j-community-5.26.9\neo4j-community-5.26.9"
powershell -NoProfile -Command "$enc = New-Object System.Text.UTF8Encoding($false); [Console]::InputEncoding = $enc; [Console]::OutputEncoding = $enc; $OutputEncoding = $enc; $q = [Console]::In.ReadToEnd(); $q = $q.TrimStart([char]0xFEFF); $q | & 'E:\Java\Java 17\bin\java.exe' '-classpath' '%NEO4J_HOME%\etc;%NEO4J_HOME%\lib\*' '-Dapp.name=cypher-shell' '-Dapp.repo=%NEO4J_HOME%\lib' '-Dapp.home=%NEO4J_HOME%' '-Dbasedir=%NEO4J_HOME%' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' 'org.neo4j.shell.startup.CypherShellBoot' '-u' 'neo4j' '-p' 'ppsxngS4' '--format' 'plain'"
exit /b 0
