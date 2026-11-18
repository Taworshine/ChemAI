@echo off
netsh advfirewall firewall add rule name="mind-server-8080" dir=in action=allow protocol=TCP localport=8080
netsh advfirewall firewall add rule name="mind-server-8080-udp" dir=in action=allow protocol=UDP localport=8080
echo.
echo 防火墙规则已添加，按任意键退出
pause
