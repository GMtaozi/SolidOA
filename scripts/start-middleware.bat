@echo off
chcp 65001 >nul

cd /d D:\Project\SolidOA\solidoa\docker

echo Starting Middleware Services...
docker-compose up -d

echo.
echo Waiting for services to be healthy...
timeout /t 15 /nobreak >nul

echo.
echo Checking services...
docker-compose ps

echo.
echo Middleware started successfully!
echo.
pause