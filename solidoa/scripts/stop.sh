#!/bin/bash
# SolidOA 停止脚本

set -e

APP_HOME="/opt/solidoa"
SERVICES=("gateway" "system" "workflow" "collaboration" "finance" "attendance" "file" "dingtalk")

echo "正在停止 SolidOA 服务..."

for service in "${SERVICES[@]}"; do
    if [ -f "${APP_HOME}/app/${service}.pid" ]; then
        pid=$(cat ${APP_HOME}/app/${service}.pid)
        if kill -0 $pid 2>/dev/null; then
            echo "停止 ${service} (PID: $pid)..."
            kill $pid
            sleep 2

            # 强制停止
            if kill -0 $pid 2>/dev/null; then
                kill -9 $pid 2>/dev/null || true
            fi
        fi
        rm -f ${APP_HOME}/app/${service}.pid
    fi
done

echo "所有服务已停止"