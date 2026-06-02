# SolidOA 企业办公自动化系统

[![Backend CI](https://github.com/GMtaozi/SolidOA/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/GMtaozi/SolidOA/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/GMtaozi/SolidOA/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/GMtaozi/SolidOA/actions/workflows/frontend-ci.yml)

## 项目简介

SolidOA 是一套基于 Spring Cloud Alibaba 微服务架构的企业办公自动化系统，支持审批流程、协作办公、财务管理、考勤管理、文件管理和钉钉集成等功能。系统采用前后端分离架构，移动端支持 UniApp。

## 技术架构

### 后端技术栈
- **核心框架**: Spring Boot 3.2.x / Spring Cloud Alibaba 2023.x
- **ORM框架**: MyBatis-Plus 3.5.x
- **工作流引擎**: Camunda 7.21
- **消息队列**: RabbitMQ 3.x
- **缓存**: Redis 7.x
- **数据库**: MySQL 8.0
- **熔断降级**: Sentinel 1.8.x
- **对象存储**: MinIO

### 前端技术栈
- Vue3 + Vite + TypeScript
- Element Plus UI
- Pinia 状态管理

### 移动端
- UniApp (iOS/Android/小程序/H5)

## 服务架构

```
┌─────────────────────────────────────────────────────┐
│                    Gateway (8080)                    │
│              JWT 认证 / 限流 / 路由转发               │
└─────────────────────────────────────────────────────┘
                              │
    ┌─────────┬─────────┬─────────┬─────────┬─────────┐
    ▼         ▼         ▼         ▼         ▼         ▼
 ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐
 │System│ │Workflow│ │Collab│ │Finance│ │Attend│ │File │ │Dingtalk│
 │8081 │  │8082  │  │8083 │  │8084  │  │8085 │  │8086 │ │ 8087  │
 └────┘  └────┘  └────┘  └────┘  └────┘  └────┘  └────┘
```

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| solidoa-gateway | 8080 | API 网关，统一入口 |
| solidoa-system | 8081 | 系统管理（用户、角色、权限、部门、数据字典） |
| solidoa-workflow | 8082 | 审批流程（请假、报销） |
| solidoa-collaboration | 8083 | 协作办公（消息、日程、通讯录） |
| solidoa-finance | 8084 | 财务管理（预算、报销统计） |
| solidoa-attendance | 8085 | 考勤管理（打卡、补卡） |
| solidoa-file | 8086 | 文件服务（上传、下载、预览） |
| solidoa-dingtalk | 8087 | 钉钉集成 |

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0
- Redis 7.x
- RabbitMQ 3.x
- MinIO（可选，用于文件存储）

### 1. 初始化数据库

```bash
# 创建数据库
mysql -u root -p < sql/init/00_init_databases.sql

# 导入表结构
mysql -u root -p oa_system < sql/init/01_system.sql
mysql -u root -p oa_workflow < sql/init/02_workflow.sql
mysql -u root -p oa_collaboration < sql/init/03_collaboration.sql
mysql -u root -p oa_finance < sql/init/04_finance.sql
mysql -u root -p oa_attendance < sql/init/05_attendance.sql
mysql -u root -p oa_common < sql/init/06_common.sql

# 导入初始化数据
mysql -u root -p oa_system < sql/init/07_init_data.sql
```

### 2. 编译项目

```bash
cd solidoa
mvn clean package -DskipTests
```

### 3. 配置数据库连接

修改各服务 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oa_system?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
```

### 4. 启动服务

```bash
cd scripts
./start-services.bat   # Windows
# 或
./deploy.sh           # Linux/Mac
```

### 5. 验证服务

```bash
curl http://localhost:8081/api/v1/system/users/1
curl http://localhost:8082/api/v1/workflow/leave
curl http://localhost:8084/api/v1/finance/budget
```

## 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 超级管理员 |

## 主要功能

### 系统管理 (System)
- 用户CRUD
- 角色权限管理
- 部门管理
- 数据字典

### 审批流程 (Workflow)
- 请假申请/审批
- 报销申请/审批
- 审批流程配置
- 任务查询

### 协作办公 (Collaboration)
- 消息通知
- 日程管理
- 通讯录

### 财务管理 (Finance)
- 预算管理
- 报销统计
- 预算预警

### 考勤管理 (Attendance)
- 打卡签到
- 补卡申请
- 月度汇总
- 异常查询

### 文件服务 (File)
- 文件上传
- 文件下载
- 在线预览

## 项目结构

```
solidoa/
├── solidoa-parent/              # 父模块
│   ├── solidoa-common/         # 公共模块（实体、工具类）
│   ├── solidoa-gateway/        # 网关服务
│   ├── solidoa-system/         # 系统管理服务
│   ├── solidoa-workflow/       # 审批流程服务
│   ├── solidoa-collaboration/  # 协作办公服务
│   ├── solidoa-finance/        # 财务管理服务
│   ├── solidoa-attendance/     # 考勤管理服务
│   ├── solidoa-file/           # 文件服务
│   └── solidoa-dingtalk/       # 钉钉集成服务
├── sql/                        # 数据库脚本
│   └── init/                   # 初始化脚本
├── scripts/                    # 部署脚本
├── docker/                    # Docker配置
└── docs/                      # 项目文档
```

## 文档目录

- [API文档](docs/API.md) - 完整的API接口说明
- [部署文档](docs/DEPLOYMENT.md) - 详细部署指南

## 开发指南

### 添加新服务

1. 在 `solidoa-parent/pom.xml` 中添加模块
2. 创建服务目录结构
3. 配置 `application.yml`
4. 添加 Mapper/Service/Controller

### 代码规范

- 命名：驼峰命名
- 分层：Controller → Service → Mapper
- 异常处理：使用 GlobalExceptionHandler

## License

MIT License
