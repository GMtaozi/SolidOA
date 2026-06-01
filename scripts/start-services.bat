@echo off
chcp 65001 >nul

cd /d D:\Project\SolidOA

echo Starting SolidOA Services with environment variables...
echo.

:: 设置环境变量
set JWT_SECRET=SolidOA2024SecretKeyForJWTTokenGeneration
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=
set CORS_ORIGINS=*
set DB_HOST=localhost
set DB_PORT=3307
set DB_USER=root
set DB_PASSWORD=749958714
set DB_NAME=oa_system
set RABBITMQ_HOST=localhost
set RABBITMQ_PORT=5672
set RABBITMQ_USER=guest
set RABBITMQ_PASSWORD=guest

:: 停止现有的 Java 服务
echo Stopping existing services...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul

:: 启动 Gateway
echo Starting Gateway (8080)...
start "SolidOA-Gateway" cmd /c "set JWT_SECRET=%JWT_SECRET% && set REDIS_HOST=%REDIS_HOST% && set REDIS_PORT=%REDIS_PORT% && set REDIS_PASSWORD=%REDIS_PASSWORD% && set CORS_ORIGINS=%CORS_ORIGINS% && java -jar solidoa\solidoa-parent\solidoa-gateway\target\solidoa-gateway-1.0.0-SNAPSHOT.jar"
timeout /t 8 /nobreak >nul

:: 启动 System
echo Starting System (8081)...
start "SolidOA-System" cmd /c "set DB_HOST=%DB_HOST% && set DB_PORT=%DB_PORT% && set DB_USER=%DB_USER% && set DB_PASSWORD=%DB_PASSWORD% && set DB_NAME=oa_system && java -jar solidoa\solidoa-parent\solidoa-system\target\solidoa-system-1.0.0-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul

:: 启动 Workflow
echo Starting Workflow (8082)...
start "SolidOA-Workflow" cmd /c "set DB_HOST=%DB_HOST% && set DB_PORT=%DB_PORT% && set DB_USER=%DB_USER% && set DB_PASSWORD=%DB_PASSWORD% && set DB_NAME=oa_workflow && java -jar solidoa\solidoa-parent\solidoa-workflow\target\solidoa-workflow-1.0.0-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul

:: 启动 Collaboration
echo Starting Collaboration (8083)...
start "SolidOA-Collaboration" cmd /c "set DB_HOST=%DB_HOST% && set DB_PORT=%DB_PORT% && set DB_USER=%DB_USER% && set DB_PASSWORD=%DB_PASSWORD% && set DB_NAME=oa_collaboration && java -jar solidoa\solidoa-parent\solidoa-collaboration\target\solidoa-collaboration-1.0.0-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul

:: 启动 Finance
echo Starting Finance (8084)...
start "SolidOA-Finance" cmd /c "set DB_HOST=%DB_HOST% && set DB_PORT=%DB_PORT% && set DB_USER=%DB_USER% && set DB_PASSWORD=%DB_PASSWORD% && set DB_NAME=oa_workflow && java -jar solidoa\solidoa-parent\solidoa-finance\target\solidoa-finance-1.0.0-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul

:: 启动 Attendance
echo Starting Attendance (8085)...
start "SolidOA-Attendance" cmd /c "set DB_HOST=%DB_HOST% && set DB_PORT=%DB_PORT% && set DB_USER=%DB_USER% && set DB_PASSWORD=%DB_PASSWORD% && set DB_NAME=oa_attendance && java -jar solidoa\solidoa-parent\solidoa-attendance\target\solidoa-attendance-1.0.0-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul

:: 启动 File
echo Starting File (8086)...
start "SolidOA-File" cmd /c "set MINIO_ENDPOINT=%MINIO_ENDPOINT% && set MINIO_ACCESS_KEY=%MINIO_ACCESS_KEY% && set MINIO_SECRET_KEY=%MINIO_SECRET_KEY% && java -jar solidoa\solidoa-parent\solidoa-file\target\solidoa-file-1.0.0-SNAPSHOT.jar"

echo.
echo ========================================
echo All services started!
echo Checking health...
timeout /t 5 /nobreak >nul
curl -s http://localhost:8080/actuator/health 2>&1 || echo Gateway may still be starting...
echo.
pause
