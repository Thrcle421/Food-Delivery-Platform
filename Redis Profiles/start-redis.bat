@echo off
cd /d D:\Redis-x64-5.0.14.1
echo Starting Redis instances...

echo Starting master node (6379)...
start "Redis-6379" redis-server.exe conf\redis-6379.conf
timeout /t 3 /nobreak

echo Starting slave node 1 (6380)...
start "Redis-6380" redis-server.exe conf\redis-6380.conf
timeout /t 2 /nobreak

echo Starting slave node 2 (6381)...
start "Redis-6381" redis-server.exe conf\redis-6381.conf
timeout /t 2 /nobreak

echo Checking cluster status...
redis-cli -p 6379 info replication

echo Redis cluster started!
pause