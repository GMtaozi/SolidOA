@echo off
chcp 65001 >nul
REM ===============================================
REM SolidOA API 测试脚本
REM 环境：本地开发环境
REM 依赖：Docker Compose 已启动
REM ===============================================

setlocal enabledelayedexpansion

set "BASE_URL=http://localhost"
set "GATEWAY_PORT=8080"

echo ===============================================
echo SolidOA API 测试脚本
echo ===============================================
echo.

REM 检查服务是否可用
echo [1/6] 检查服务健康状态...

REM 检查 Gateway
curl -s -o nul -w "%%{http_code}" "%BASE_URL%:%GATEWAY_PORT%/actuator/health" | findstr /C:"200" >nul
if errorlevel 1 (
    echo [错误] Gateway 服务不可用，请检查服务是否启动
    echo 提示：Gateway 应在 %GATEWAY_PORT% 端口
    exit /b 1
)
echo [OK] Gateway 服务正常

echo.
echo ===============================================
echo 测试完成
echo ===============================================

pause