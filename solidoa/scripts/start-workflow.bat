@echo off
chcp 65001 >nul
echo ==========================================
echo 启动 solidoa-workflow 服务...
echo ==========================================

cd /d D:\Project\SolidOA\solidoa

REM 启动服务
start "Workflow-8082" cmd /c "java -jar solidoa-parent\solidoa-workflow\target\solidoa-workflow-1.0.0-SNAPSHOT.jar --server.port=8082"

echo 等待30秒让服务启动...
timeout /t 30

echo.
echo 测试 API...
curl -s -w "\nHTTP_CODE:%%{http_code}" http://localhost:8082/api/v1/workflow/leave -H "X-User-Id: 1"

echo.
echo.
echo 服务状态检查:
for %%p in (8080 8081 8082 8083 8084 8085) do (
    curl -s -o nul -w "Port %%p: %%{http_code}\n" --connect-timeout 2 http://localhost:%%p/actuator/health
)

pause