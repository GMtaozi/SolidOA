@echo off
chcp 65001 >nul

cd /d D:\Project\SolidOA\solidoa\docker

echo Stopping Middleware Services...
docker-compose down

echo.
echo Middleware stopped!
pause