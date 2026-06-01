# SolidOA 微服务启动指南

## 一、环境准备

### 1.1 启动中间件
```bash
cd solidoa/docker
docker compose up -d
```

验证服务状态：
```bash
docker compose ps
```

### 1.2 检查端口占用
| 服务 | 端口 | 检查命令 |
|------|------|----------|
| Nacos | 8848 | `curl http://localhost:8848/nacos/v1/console/health` |
| MySQL | 3307 | `docker port solidoa-mysql` |
| Redis | 6379 | `redis-cli ping` |
| RabbitMQ | 5672/15672 | `docker port solidoa-rabbitmq` |
| MinIO | 9000/9001 | `docker port solidoa-minio` |

## 二、启动微服务

### 2.1 IDEA 启动（推荐）
1. 打开项目 `solidoa-parent`
2. 按顺序启动：

| 顺序 | 服务 | 主类 | 端口 |
|------|------|------|------|
| 1 | Gateway | `GatewayApplication` | 8080 |
| 2 | System | `SystemApplication` | 8081 |
| 3 | Workflow | `WorkflowApplication` | 8082 |
| 4 | Collaboration | `CollabApplication` | 8083 |
| 5 | Finance | `FinanceApplication` | 8084 |
| 6 | Attendance | `AttendanceApplication` | 8085 |
| 7 | File | `FileApplication` | 8086 |
| 8 | DingTalk | (独立) | 8087 |

### 2.2 命令行启动
```bash
cd solidoa-parent

# 编译
mvn clean compile -DskipTests

# 启动 Gateway
mvn spring-boot:run -pl solidoa-gateway

# 新开终端，启动 System
mvn spring-boot:run -pl solidoa-system
```

### 2.3 Maven 启动脚本 (Linux/Mac)
```bash
#!/bin/bash
modules=("solidoa-gateway" "solidoa-system" "solidoa-workflow" "solidoa-collaboration" "solidoa-finance" "solidoa-attendance" "solidoa-file")

for module in "${modules[@]}"; do
    echo "Starting $module..."
    mvn spring-boot:run -pl "$module" &
done

wait
```

## 三、验证启动

### 3.1 检查服务注册
访问 Nacos 控制台：http://localhost:8848/nacos
- 用户名：nacos
- 密码：nacos

### 3.2 API 健康检查
```bash
# Gateway
curl http://localhost:8080/actuator/health

# System
curl http://localhost:8081/actuator/health

# Workflow
curl http://localhost:8082/actuator/health
```

### 3.3 运行 API 测试
```bash
cd scripts
./test-api.sh
```

## 四、常见问题

### 4.1 Nacos 连接失败
- 检查 `docker compose ps` 确认 Nacos 容器状态
- 确认 8848 端口未被占用

### 4.2 数据库连接失败
- 确认 MySQL 容器已启动且 healthy
- 检查数据库 `solidoa` 是否已创建

### 4.3 Redis 连接失败
- 确认 Redis 容器已启动
- 检查密码配置（默认无密码）

### 4.4 服务启动超时
- 增加 JVM 内存：`mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m"`

## 五、项目结构

```
solidoa/
├── solidoa-parent/          # 父模块
│   ├── solidoa-common/      # 公共模块
│   ├── solidoa-gateway/     # API 网关
│   ├── solidoa-system/      # 系统管理
│   ├── solidoa-workflow/    # 审批流程
│   ├── solidoa-collaboration/ # 消息中心
│   ├── solidoa-finance/     # 财务管理
│   ├── solidoa-attendance/  # 考勤管理
│   └── solidoa-file/        # 文件服务
├── solidoa-dingtalk/         # 钉钉集成（独立模块）
├── solidoa-vue/             # Vue3 前端
├── solidoa-uniapp/          # 移动端
└── docker/                  # Docker 配置
```