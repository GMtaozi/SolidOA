@echo off
chcp 65001 >nul
REM ===============================================
REM SolidOA 服务启动器 (UTF-8 编码)
REM 双击此文件启动所有服务，终端自动使用 UTF-8
REM ===============================================

echo ===============================================
echo SolidOA 服务启动器
echo ===============================================
echo.

REM 设置 Java UTF-8 编码
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

REM 配置
set "PROJECT_DIR=%~dp0..\solidoa-parent"

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 Java，请安装 JDK 17+
    pause
    exit /b 1
)
echo [OK] Java 已安装

echo.
echo 启动服务顺序:
echo   1. Gateway  (8080)
echo   2. System   (8081)
echo   3. Workflow (8082)
echo.

set /p choice="是否启动所有服务? (Y/N): "
if /i not "%choice%"=="Y" (
    echo 已取消
    pause
    exit /b 0
)

echo.
echo [1/3] 启动 Gateway...
cd /d "%PROJECT_DIR%"
start "Gateway-8080" cmd /k "chcp 65001 >nul && title Gateway-8080 && mvn spring-boot:run -pl solidoa-gateway -DskipTests"

echo [2/3] 启动 System (5秒后)...
timeout /t 5 /nobreak >nul
start "System-8081" cmd /k "chcp 65001 >nul && title System-8081 && mvn spring-boot:run -pl solidoa-system -DskipTests"

echo [3/3] 启动 Workflow (5秒后)...
timeout /t 5 /nobreak >nul
start "Workflow-8082" cmd /k "chcp 65001 >nul && title Workflow-8082 && mvn spring-boot:run -pl solidoa-workflow -DskipTests"

echo.
echo ===============================================
echo 所有服务已启动，每个服务在独立窗口中运行
echo 关闭此窗口不影响服务运行
echo ===============================================
timeout /t 3 /nobreak >nul
