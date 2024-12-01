@echo off
cd /d D:\Redis-x64-5.0.14.1
echo Stopping Redis instances...

redis-cli -p 6381 shutdown
redis-cli -p 6380 shutdown
redis-cli -p 6379 shutdown

echo Redis cluster stopped!
pause