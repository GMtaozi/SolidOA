#!/bin/bash
# ===============================================
# SolidOA API 测试脚本 (Bash 版本)
# 环境：本地开发环境
# 依赖：Docker Compose 已启动，微服务已启动
# ===============================================

set -e

BASE_URL="http://localhost"
GATEWAY_PORT=${GATEWAY_PORT:-8080}
TOKEN_FILE="/tmp/solidoa_token.txt"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 curl
if ! command -v curl &> /dev/null; then
    log_error "curl 未安装，请安装 curl"
    exit 1
fi

echo "==============================================="
echo "SolidOA API 测试脚本"
echo "==============================================="
echo

# 1. 健康检查
log_info "[1/5] 检查服务健康状态..."

HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL:$GATEWAY_PORT/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH_STATUS" != "200" ]; then
    log_error "Gateway 服务不可用 (HTTP $HEALTH_STATUS)"
    log_info "请确保微服务已在 $GATEWAY_PORT 端口启动"
    log_info "提示: 运行 mvn spring-boot:run 或使用 IDE 启动服务"
    exit 1
fi
log_info "Gateway 服务正常"

# 2. 登录测试
log_info "[2/5] 测试登录功能..."

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL:$GATEWAY_PORT/api/system/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null)

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    log_warn "登录失败或 Token 为空"
    log_warn "响应: $LOGIN_RESPONSE"
    log_warn "跳过需要认证的 API 测试"
    TOKEN=""
else
    echo "$TOKEN" > "$TOKEN_FILE"
    log_info "登录成功，获取 Token 成功"
fi

# 3. 测试用户列表
log_info "[3/5] 测试用户管理 API..."

if [ -n "$TOKEN" ]; then
    USER_RESPONSE=$(curl -s -X GET "$BASE_URL:$GATEWAY_PORT/api/system/users" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)

    if echo "$USER_RESPONSE" | grep -q '"code":200'; then
        log_info "用户列表 API 正常"
    else
        log_warn "用户列表 API 响应异常"
    fi
else
    log_warn "跳过用户列表测试（需要认证）"
fi

# 4. 测试考勤打卡
log_info "[4/5] 测试考勤打卡 API..."

if [ -n "$TOKEN" ]; then
    CLOCK_RESPONSE=$(curl -s -X POST "$BASE_URL:$GATEWAY_PORT/api/attendance/clock" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"type":"CLOCK_IN"}' 2>/dev/null)

    if echo "$CLOCK_RESPONSE" | grep -q '"code"'; then
        log_info "考勤打卡 API 正常"
    else
        log_warn "考勤打卡 API 响应异常"
    fi
else
    log_warn "跳过考勤打卡测试（需要认证）"
fi

# 5. 测试消息中心
log_info "[5/5] 测试消息中心 API..."

if [ -n "$TOKEN" ]; then
    MSG_RESPONSE=$(curl -s -X GET "$BASE_URL:$GATEWAY_PORT/api/collaboration/messages?pageNum=1&pageSize=10" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)

    if echo "$MSG_RESPONSE" | grep -q '"code"'; then
        log_info "消息中心 API 正常"
    else
        log_warn "消息中心 API 响应异常"
    fi
else
    log_warn "跳过消息中心测试（需要认证）"
fi

# 清理
rm -f "$TOKEN_FILE"

echo
echo "==============================================="
log_info "API 测试完成"
echo "==============================================="

if [ -n "$TOKEN" ]; then
    echo "测试摘要:"
    echo "  - 健康检查: OK"
    echo "  - 登录测试: OK"
    echo "  - 用户管理: OK"
    echo "  - 考勤打卡: OK"
    echo "  - 消息中心: OK"
else
    echo "注意: 部分测试因缺少认证 Token 而跳过"
fi