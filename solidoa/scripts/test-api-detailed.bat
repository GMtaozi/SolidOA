@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ===============================================
REM SolidOA 模块 API 详细测试脚本
REM ===============================================

set "BASE_URL=http://localhost"
set "GATEWAY_PORT=8080"

echo ===============================================
echo SolidOA API 详细测试
echo ===============================================
echo.

REM 颜色代码
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

REM 测试结果
set PASSED=0
set FAILED=0
set SKIPPED=0

REM 登录获取Token
echo [1/10] 登录测试...
for /f "tokens=*" %%i in ('curl -s -X POST "%BASE_URL%:%GATEWAY_PORT%/api/system/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"') do set LOGIN_RESP=%%i
echo 响应: !LOGIN_RESP:~0,100!...

REM 提取Token（简化处理）
echo.
echo ===============================================
echo 测试结果摘要
echo ===============================================
echo 通过: %PASSED%
echo 失败: %FAILED%
echo 跳过: %SKIPPED%
echo ===============================================

pause