#!/bin/bash
# SolidOA 健康检查脚本

set -e

SERVICES=(
    "gateway:8080"
    "system:8081"
    "workflow:8082"
    "collaboration:8083"
    "finance:8084"
    "attendance:8085"
    "file:8086"
    "dingtalk:8087"
)

all_healthy=true

echo "===== SolidOA 健康检查 ====="
echo ""

for item in "${SERVICES[@]}"; do
    name="${item%%:*}"
    port="${item##*:}"

    if curl -s --connect-timeout 5 "http://localhost:${port}/actuator/health" | grep -q UP; then
        echo -e "[✓] ${name} \033[0;32m健康\033[0m (端口: ${port})"
    else
        echo -e "[✗] ${name} \033[0;31m不健康\033[0m (端口: ${port})"
        all_healthy=false
    fi
done

echo ""
if $all_healthy; then
    echo "===== 所有服务健康 ====="
    exit 0
else
    echo "===== 部分服务不健康 ====="
    exit 1
fi