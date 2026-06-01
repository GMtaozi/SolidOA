#!/bin/bash
# SolidOA 部署脚本

set -e

# 配置
APP_HOME="${SOLIDOA_HOME:-/opt/solidoa}"
LOG_HOME="${APP_HOME}/logs"
APP_NAME="solidoa"
VERSION=${1:-latest}

# 敏感配置（必须通过环境变量传入）
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"  # 必须设置环境变量

REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

NACOS_HOST="${NACOS_HOST:-localhost}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查敏感配置
check_config() {
    if [ -z "$MYSQL_PASSWORD" ]; then
        log_error "MYSQL_PASSWORD 环境变量未设置"
        log_error "请设置: export MYSQL_PASSWORD='your_password'"
        exit 1
    fi

    if [ -z "$NACOS_PASSWORD" ]; then
        log_warn "NACOS_PASSWORD 环境变量未设置，使用默认值"
        NACOS_PASSWORD="nacos"
    fi
}

# 检查是否为 root 用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "请使用 root 用户或 sudo 执行此脚本"
        exit 1
    fi
}

# 创建目录
create_dirs() {
    log_info "创建目录..."
    mkdir -p ${APP_HOME}/{app,config,logs}
    mkdir -p ${LOG_HOME}/{gateway,system,workflow,collaboration,finance,attendance,file,dingtalk}
}

# 等待基础设施启动
wait_for_services() {
    log_info "等待基础设施服务..."

    # 等待 Nacos
    local max_attempts=60
    local attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/console/health" | grep -q UP; then
            log_info "Nacos 已就绪"
            break
        fi
        attempt=$((attempt + 1))
        log_info "等待 Nacos... (${attempt}/${max_attempts})"
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        log_error "Nacos 启动超时"
        exit 1
    fi

    # 等待 MySQL
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "SELECT 1" &>/dev/null; then
            log_info "MySQL 已就绪"
            break
        fi
        attempt=$((attempt + 1))
        log_info "等待 MySQL... (${attempt}/${max_attempts})"
        sleep 2
    done

    # 等待 Redis
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_PASSWORD:+-a "${REDIS_PASSWORD}"} ping &>/dev/null; then
            log_info "Redis 已就绪"
            break
        fi
        attempt=$((attempt + 1))
        log_info "等待 Redis... (${attempt}/${max_attempts})"
        sleep 2
    done
}

# 启动服务
start_service() {
    local service_name=$1
    local service_port=$2
    local jar_name=$3

    log_info "启动 ${service_name}..."

    nohup java -Xms256m -Xmx512m -XX:+UseG1GC \
        -Dspring.profiles.active=prod \
        -Dserver.port=${service_port} \
        -Dspring.cloud.nacos.server-addr="${NACOS_HOST}:${NACOS_PORT}" \
        -Dspring.cloud.nacos.username="${NACOS_USERNAME}" \
        -Dspring.cloud.nacos.password="${NACOS_PASSWORD}" \
        -Dspring.datasource.url="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/solidoa_${service_name}?useUnicode=true&characterEncoding=utf8" \
        -Dspring.datasource.username="${MYSQL_USER}" \
        -Dspring.datasource.password="${MYSQL_PASSWORD}" \
        -Dspring.redis.host="${REDIS_HOST}" \
        -Dspring.redis.port="${REDIS_PORT}" \
        ${REDIS_PASSWORD:+-Dspring.redis.password="${REDIS_PASSWORD}"} \
        -jar ${APP_HOME}/app/${jar_name}.jar \
        --spring.config.additional-location=${APP_HOME}/config/ \
        > ${LOG_HOME}/${service_name}/stdout.log 2>&1 &

    local pid=$!
    echo $pid > ${APP_HOME}/app/${service_name}.pid

    # 健康检查
    local max_attempts=30
    local attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if curl -s --connect-timeout 5 "http://localhost:${service_port}/actuator/health" | grep -q UP; then
            log_info "${service_name} 启动成功 (PID: $pid)"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    log_error "${service_name} 启动失败，请检查日志: ${LOG_HOME}/${service_name}/stdout.log"
    return 1
}

# 主函数
main() {
    log_info "===== SolidOA 部署开始 ====="
    log_info "版本: ${VERSION}"

    check_config
    check_root
    create_dirs
    wait_for_services

    # 启动各个服务
    start_service "gateway" 8080 "solidoa-gateway"
    start_service "system" 8081 "solidoa-system"
    start_service "workflow" 8082 "solidoa-workflow"
    start_service "collaboration" 8083 "solidoa-collaboration"
    start_service "finance" 8084 "solidoa-finance"
    start_service "attendance" 8085 "solidoa-attendance"
    start_service "file" 8086 "solidoa-file"
    start_service "dingtalk" 8087 "solidoa-dingtalk"

    log_info "===== SolidOA 部署完成 ====="
    log_info "Gateway: http://localhost:8080"
    log_info "API 文档: http://localhost:8080/swagger-ui.html"
}

main "$@"