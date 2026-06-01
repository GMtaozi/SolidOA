@echo off
chcp 65001 >nul 2>&1
title SolidOA Service Launcher

cd /d D:\Project\SolidOA
if errorlevel 1 (
    echo [ERROR] Cannot access D:\Project\SolidOA
    pause
    exit /b 1
)

echo ========================================
echo   SolidOA Service Launcher
echo ========================================
echo.

:: Create logs directory
if not exist logs mkdir logs

:: Set environment variables
set JWT_SECRET=SolidOA2024SecretKeyForJWTTokenGeneration
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=
set CORS_ORIGINS=*
set DB_HOST=localhost
set DB_PORT=3307
set DB_USER=root
set DB_PASSWORD=749958714
set RABBITMQ_HOST=localhost
set RABBITMQ_PORT=5672
set RABBITMQ_USER=guest
set RABBITMQ_PASSWORD=guest
set MINIO_ENDPOINT=http://localhost:9000
set MINIO_ACCESS_KEY=minioadmin
set MINIO_SECRET_KEY=minioadmin123
set LOG_PATH=D:\Project\SolidOA\logs

:: Check Docker
echo Checking Docker...
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    pause
    exit /b 1
)

:: Check MySQL container
docker ps --filter "name=solidoa-mysql" --filter "status=running" --format "{{.Names}}" | findstr solidoa-mysql >nul
if errorlevel 1 (
    echo [ERROR] MySQL container not running!
    echo Please run: scripts\start-middleware.bat
    pause
    exit /b 1
)
echo [OK] Docker and MySQL are running
echo.

:: Stop existing Java services
echo Stopping existing Java services...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 3 /nobreak >nul
echo.

:: Start services
echo Starting services...
echo.

echo [1/5] Starting Gateway on port 8080...
set DB_NAME=oa_system
start "SolidOA-Gateway" /D "D:\Project\SolidOA" java -jar solidoa\solidoa-parent\solidoa-gateway\target\solidoa-gateway-1.0.0-SNAPSHOT.jar
timeout /t 10 /nobreak >nul

echo [2/5] Starting System on port 8081...
set DB_NAME=oa_system
start "SolidOA-System" /D "D:\Project\SolidOA" java -jar solidoa\solidoa-parent\solidoa-system\target\solidoa-system-1.0.0-SNAPSHOT.jar
timeout /t 8 /nobreak >nul

echo [3/5] Starting Workflow on port 8082...
set DB_NAME=oa_workflow
start "SolidOA-Workflow" /D "D:\Project\SolidOA" java -Dfile.encoding=UTF-8 -jar solidoa\solidoa-parent\solidoa-workflow\target\solidoa-workflow-1.0.0-SNAPSHOT.jar
timeout /t 8 /nobreak >nul

echo [4/5] Starting HR on port 8085...
set DB_NAME=oa_hr
start "SolidOA-HR" /D "D:\Project\SolidOA" java -jar solidoa\solidoa-parent\solidoa-hr\target\solidoa-hr-1.0.0-SNAPSHOT.jar
timeout /t 8 /nobreak >nul

echo [5/5] Starting File on port 8086...
set DB_NAME=oa_file
start "SolidOA-File" /D "D:\Project\SolidOA" java -jar solidoa\solidoa-parent\solidoa-file\target\solidoa-file-1.0.0-SNAPSHOT.jar

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Waiting for services to initialize...
timeout /t 15 /nobreak >nul

echo Service Status:
echo ----------------------------------------

curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel%==0 (echo   [Gateway]   : UP) else (echo   [Gateway]   : DOWN)

curl -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel%==0 (echo   [System]    : UP) else (echo   [System]    : DOWN)

curl -s http://localhost:8082/actuator/health >nul 2>&1
if %errorlevel%==0 (echo   [Workflow]  : UP) else (echo   [Workflow]  : DOWN)

curl -s http://localhost:8085/actuator/health >nul 2>&1
if %errorlevel%==0 (echo   [HR]        : UP) else (echo   [HR]        : DOWN)

curl -s http://localhost:8086/actuator/health >nul 2>&1
if %errorlevel%==0 (echo   [File]      : UP) else (echo   [File]      : DOWN)

echo.
echo ========================================
echo Frontend: http://localhost:3000
echo Gateway:  http://localhost:8080
echo ========================================
echo.
pause
