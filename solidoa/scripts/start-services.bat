@echo off
chcp 65001 >nul
REM ===============================================
REM SolidOA 微服务启动脚本 (Windows)
REM ===============================================

echo ===============================================
echo SolidOA 微服务启动脚本
echo ===============================================
echo.

REM 配置
set "PROJECT_DIR=%~dp0..\solidoa-parent"
set "START_PORT=8080"
set "NACOS_ADDR=http://localhost:8848"

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java，请安装 JDK 17+
    exit /b 1
)
echo [OK] Java 已安装

REM 检查 Docker 服务
docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行
    exit /b 1
)

REM 检查中间件
echo.
echo [1/5] 检查中间件服务...
curl -s -o nul -w "%%{http_code}" "http://localhost:8848/nacos/v1/console/health" | findstr /C:"200" >nul
if errorlevel 1 (
    echo [警告] Nacos 未就绪，请确保 docker-compose 已启动
    echo 运行: cd docker ^&^& docker compose up -d
    exit /b 1
)
echo [OK] Nacos 就绪

curl -s -o nul -w "%%{http_code}" "http://localhost:3307" | findstr /C:"200" >nul
if errorlevel 1 (
    echo [警告] MySQL 未就绪
)

curl -s -o nul -w "%%{http_code}" "http://localhost:6379" | findstr /C:"200" >nul
if errorlevel 1 (
    echo [警告] Redis 未就绪
)

echo.
echo [2/5] Maven 编译项目...
cd /d "%PROJECT_DIR%"
call mvn clean compile -DskipTests -q
if errorlevel 1 (
    echo [错误] 编译失败
    exit /b 1
)
echo [OK] 编译完成

echo.
echo [3/5] 微服务启动顺序：
echo   1. Gateway (8080)
echo   2. System (8081)
echo   3. Workflow (8082)
echo   4. Collaboration (8083)
echo   5. Finance (8084)
echo   6. Attendance (8085)
echo   7. File (8086)
echo   8. DingTalk (8087)
echo.

echo [4/5] 启动微服务...
echo 注意：建议在 IDEA/Eclipse 中启动以获得更好的日志输出
echo.

REM 提示用户
echo [5/5] 启动说明：
echo.
echo 方法1: IDEA 启动
echo   1. 打开项目
echo   2. 运行各模块的 *Application.java
echo   3. 按顺序启动：Gateway ^> System ^> 其他服务
echo.
echo 方法2: Maven 启动（单服务测试）
echo   cd solidoa-parent
echo   mvn spring-boot:run -pl solidoa-gateway
echo.
echo 方法3: Docker 部署（生产环境）
echo   cd docker
echo   docker compose up -d

echo.
echo ===============================================
echo 启动准备完成
echo ===============================================
pause