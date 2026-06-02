# SolidOA 企业办公自动化系统

> 江苏德信基因检测科技有限公司 OA 系统
> 文档版本：V2.0（与 SSD_V2.0 同步）
> 最近更新：2026-06-02

## 项目简介

SolidOA 是一套基于 **Spring Cloud Alibaba 2023** 微服务架构的企业办公自动化系统，支持审批流程、协作办公、财务管理、考勤管理、文件管理、钉钉集成等功能。系统采用前后端分离架构，移动端支持 UniApp。

**核心理念**：自研轻量级状态机驱动审批流（V2.0 起替代 Camunda 重量级引擎），单轨 Feign 跨服务调用，统一权限模型 + 数据权限，全链路 TraceId 可观测。

## 技术架构

### 后端技术栈（V2.0）
| 类别 | 技术 | 版本 |
|---|---|---|
| 核心框架 | Spring Boot | 3.2.x |
| 微服务 | Spring Cloud Alibaba | 2023.0.1.2 |
| 服务发现/配置 | Nacos | 2.2.3 |
| RPC | OpenFeign | (随 SCA) |
| ORM | MyBatis-Plus | 3.5.x |
| 审批流引擎 | **自研 ApprovalStateMachine**（V2.0 替代 Camunda） | — |
| 消息队列 | RabbitMQ | 3.12 |
| 缓存 | Redis | 7.x |
| 数据库 | MySQL | 8.0 |
| 分布式事务 | **Seata AT 模式**（V2.0 新增） | 1.5.2 |
| 限流熔断 | **Sentinel** + Dashboard | 1.8.7 |
| 监控 | **Prometheus + Grafana**（V2.0 新增） | latest |
| 对象存储 | MinIO | latest |
| 实时推送 | **Spring WebSocket**（V2.0 第 9 章） | (随 Spring) |
| 权限模型 | Spring Security + `@PreAuthorize` + `X-User-Id` header | — |

### 前端技术栈
| 类别 | 技术 |
|---|---|
| 框架 | Vue 3.4 + Vite 5 + TypeScript 5 |
| UI | Element Plus 2.14 |
| 状态管理 | Pinia 2.3 |
| HTTP | Axios 1.16 |
| 样式 | SCSS + Design Tokens (`tokens.scss` 单一真理源) |
| 组件库 | **16 个 OaXxx 组件**（OaButton/Card/Dialog/FormDialog/StatusBadge/Table/ApprovalCard/ApprovalFlow/Empty/Pagination/SearchForm/PageHeader/Icon + 3 内部） |
| 单测 | Vitest 1.6（5 文件 28 测试） |

### 移动端
- UniApp (iOS/Android/小程序/H5)

## 服务架构（8 个微服务）

```
┌──────────────────────────────────────────────────────────────┐
│                       Gateway (8080)                         │
│            TraceId 注入 / Sentinel 限流 / 路由转发            │
└──────────────────────────────────────────────────────────────┘
                              │
   ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┐
   ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐
│Sys │  │Work│  │ HR │  │File │  │Ding│  │Sea │  │Sen │  │Pro │
│8081│  │8082│  │8085│  │8086│  │8087│  │8091│  │8088│  │9090│
└────┘  └────┘  └────┘  └────┘  └────┘  └────┘  └────┘  └────┘
                                                         +Grafana 3001
```

## 模块说明（实际 7 业务 + 5 基础设施）

| 模块 | 端口 | 数据库 | 职责 |
|---|---|---|---|
| **solidoa-gateway** | 8080 | — | API 网关，TraceId 注入，Sentinel 限流，路由转发 |
| **solidoa-system** | 8081 | oa_system | 用户/角色/部门/权限/字典/操作日志/通讯录/消息/日程 |
| **solidoa-workflow** | 8082 | oa_workflow | 审批流引擎（请假/报销/用印/采购/加班/出差/补卡/外出/工资） |
| **solidoa-hr** | 8085 | oa_hr | **考勤**（钉钉同步）+ **财务/工资**（合并服务） |
| **solidoa-file** | 8086 | oa_file | 文件上传/下载（MinIO） |
| **solidoa-dingtalk** | 8087 | oa_dingtalk | 钉钉集成（考勤同步/审批回调） |
| **solidoa-seata** | 8091 | — | Seata TC（事务协调器，V2.0 新增） |
| **solidoa-sentinel** | 8088 | — | Sentinel Dashboard（限流熔断控制台，V2.0 新增） |
| **solidoa-prometheus** | 9090 | — | 指标采集（V2.0 新增） |
| **solidoa-grafana** | 3001 | — | 可视化仪表盘（V2.0 新增） |

## 快速开始

### 环境要求
- **JDK 17+**
- **Maven 3.9+**
- **MySQL 8.0+**（推荐 Docker 5 库 + utf8mb4）
- **Redis 7.x+**
- **RabbitMQ 3.12+**
- **Node.js 20 LTS**（前端）
- **Docker + Docker Compose**（推荐部署方式）

### 方式 1：Docker Compose 一键启动（推荐）

```bash
# 1. 进入 docker 目录
cd solidoa/docker

# 2. 一键拉起 15 个容器 (MySQL + Redis + Nacos + RabbitMQ + MinIO + 6 Java 服务 + Seata + Sentinel + Prometheus + Grafana)
docker-compose up -d

# 3. 验证服务健康
curl http://localhost:8080/actuator/health  # 网关
curl http://localhost:8081/actuator/health  # 系统
curl http://localhost:8082/actuator/health  # 审批
```

### 方式 2：本地 IDE 开发

#### 1. 启动中间件（MySQL/Redis/Nacos/RabbitMQ）
```bash
cd solidoa/docker && docker-compose up -d mysql redis nacos rabbitmq
```

#### 2. 初始化数据库
```bash
# 5 个库：oa_system / oa_workflow / oa_hr / oa_file / oa_dingtalk
docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 \
  < solidoa/sql/init/00_init_databases.sql

# 导入表结构与种子数据
for sql in 01_system 02_workflow 03_collaboration 04_finance 05_attendance 06_common 07_init_data; do
  docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 \
    < "solidoa/sql/init/${sql}.sql"
done

# Sprint 4+ 增量 SQL
docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_system \
  < solidoa/sql/update/V2.0__permission_upgrade.sql
docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_workflow \
  < solidoa/sql/update/V4.0__message_outbox_table.sql
```

#### 3. 编译 + 启动后端
```bash
cd solidoa
mvn clean package -DskipTests

# 各服务独立启动（端口见上表）
java -jar solidoa-parent/solidoa-gateway/target/solidoa-gateway-1.0.0-SNAPSHOT.jar
java -jar solidoa-parent/solidoa-system/target/solidoa-system-1.0.0-SNAPSHOT.jar
java -jar solidoa-parent/solidoa-workflow/target/solidoa-workflow-1.0.0-SNAPSHOT.jar
# ... 等等
```

#### 4. 启动前端
```bash
cd solidoa-web
npm install
npm run dev   # http://localhost:3000
```

#### 5. 验证端点
```bash
# 登录获取 X-User-Id header
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 携带 X-User-Id 调业务接口
curl http://localhost:8080/api/v1/system/users \
  -H "X-User-Id: 1" -H "X-User-Name: admin"
```

## 默认账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `admin123` | SYSTEM_ADMIN | 超级管理员 |
| `dx001` / `dx003` / `dx010` | (系统随机) | EMPLOYEE | 测试员工 |

**密码加密**：BCrypt（`$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi` 为 admin123）

## 主要功能（按模块）

### 系统管理 (solidoa-system)
- ✅ 用户/角色/部门 CRUD + 树形展示
- ✅ 按钮级权限 `@PreAuthorize`（21 个 USER_/ROLE_/DEPT_/STAMP_/PURCHASE_/EXPENSE_/LEAVE_ 权限种子）
- ✅ 数据权限范围（`sys_role.data_scope`：1 全部 / 2 本部门 / 3 本部门及下级 / 4 本人）
- ✅ 数据字典（启动时 Redis 缓存预热，1h TTL）
- ✅ 操作日志（@Async 异步写入，可视化查询页）

### 审批流 (solidoa-workflow) — V2.0 核心
- ✅ **9 状态 / 8 事件 / 13 transitions** 自研状态机
- ✅ **4 业务**（请假/报销/用印/采购）+ **5 HR 业务**（加班/出差/补卡/外出/工资）
- ✅ **统一审批门卫** `UniversalApprovalService`：单轨守卫 + 乐观锁
- ✅ 5 端点：approve / reject / withdraw / transfer / add-sign
- ✅ 流程图（flow-graph）/ 时间线（timeline）/ 状态机信息（debug）

### 财务 + 考勤 (solidoa-hr)
- ✅ 报销管理（待审批/已审批/历史）
- ✅ 工资单（基础版，个税预扣法待 V2.0 第 8 章）
- ✅ 钉钉考勤同步（自动同步打卡记录）
- ✅ 补卡/加班/出差申请

### 协作 (solidoa-system 内)
- ✅ 通讯录（按部门树）
- ✅ 日程（CRUD + Redis 缓存 10min TTL）
- ✅ 消息中心（MessageService + 3 端点）
- 🟡 **WebSocket 实时推送**（MVP 已部署 `/ws/notification`，跨服务 Redis pub/sub 待完善）

### 文件服务 (solidoa-file)
- ✅ MinIO 上传/下载/预览

### 钉钉集成 (solidoa-dingtalk)
- 🟡 考勤数据同步（基础同步逻辑）
- ❌ 审批回调（待 V2.0 第 14 章）

## V2.0 新增能力

| 能力 | 状态 | 验收 |
|---|---|---|
| 设计系统（tokens.scss 唯一真理源） | ✅ | 全站切换主色一行生效 |
| 16 个 OaXxx 通用组件 | ✅ | 3 表单页用 OaFormDialog + 2 列表用 OaTable |
| Seata 分布式事务（容器 + Nacos 注册） | ✅ 容器，❌ 业务 AT | 跨服务强一致链路待 V2.0 第 1 章续 |
| Sentinel 限流熔断（容器 + workflow 集成） | ✅ 容器 + Feign，❌ 注解 | 6 条 flow-rules.json 就绪 |
| Prometheus + Grafana | ✅ | 5 服务 metrics 端点暴露 |
| WebSocket 实时推送 | ✅ MVP | system 端 /ws/notification 端点 |
| TraceId 全链路透传 | ✅ | Gateway Filter + 下游 Interceptor + Feign Interceptor + logback `[%X{traceId}]` |
| Redis CacheManager 5 域 | ✅ | user/dept/dict/contact/schedule/dashboard |
| 5 接口安全 @Cacheable | ✅ 4 + DictService 1 | key 含 userId 避免越权 |
| 字符集统一 | ✅ | 5 库 64 表 utf8mb4_unicode_ci |

## 项目结构

```
D:\Project\SolidOA\
├── solidoa/                          # 后端 (Maven 聚合根)
│   ├── pom.xml
│   ├── solidoa-parent/              # 子聚合根
│   │   ├── pom.xml
│   │   ├── solidoa-common/         # 共享: 实体/工具/Feign Client/TraceId
│   │   ├── solidoa-gateway/        # 8080 网关
│   │   ├── solidoa-system/         # 8081 系统管理 + 协作
│   │   ├── solidoa-workflow/       # 8082 审批引擎
│   │   ├── solidoa-hr/             # 8085 考勤 + 财务
│   │   ├── solidoa-file/           # 8086 文件
│   │   └── solidoa-dingtalk/       # 8087 钉钉
│   ├── docker/                     # docker-compose.yml + prometheus.yml + seata/sentinel 配置
│   ├── sql/
│   │   ├── init/                    # 7 库初始化脚本
│   │   └── update/                  # 增量 SQL (V1.x → V2.0)
│   └── scripts/                     # 部署脚本
├── solidoa-web/                     # 前端 Vue3 + Vite + TypeScript
│   ├── package.json
│   ├── vite.config.js
│   ├── src/
│   │   ├── api/                     # 业务 API 封装
│   │   ├── components/             # 16 个 OaXxx 组件
│   │   ├── styles/                  # tokens.scss / mixins.scss
│   │   └── views/                   # 业务页面
├── solidoa-app/                     # 移动端 UniApp H5
├── docs/                            # 项目文档
│   ├── ssd/SSD_V2.0_Optimization.md # 19 章优化蓝图
│   ├── sprint4/                     # Sprint 4 + 进度盘点
│   └── requirements/                # 需求文档
└── .github/                         # (已移除 CI 配置，按项目实际需要)
```

## 开发指南

### 添加新业务审批流（V2.0 模式）
1. 在 `solidoa-workflow/entity/` 加 entity（带 `@Version` 乐观锁）
2. 在 `solidoa-workflow/service/impl/` 加 Service 委托 `UniversalApprovalService`
3. 在 `solidoa-workflow/controller/` 加 Controller
4. 5 端点 `approve/reject/withdraw/transfer/add-sign` 全走 `UniversalApprovalService.fire()`
5. 业务副作用（写 ApprovalRecord / 发消息）保留在 Service 内

### 添加新缓存域
1. 在 `solidoa-common/.../RedisConfig.java` `cacheManager()` 加 `.withCacheConfiguration("oa:xxx", ...)`
2. 在 Service 加 `@Cacheable(value = "oa:xxx", key = "#userId + ...", unless = "#result == null")`
3. key 必须含 userId/deptId 等身份字段，**避免越权**

### 代码规范
- **命名**：驼峰命名（Java）/ kebab-case（Vue 文件）/ UPPER_SNAKE（常量）
- **分层**：Controller → Service → Mapper
- **异常处理**：`GlobalExceptionHandler`（solidoa-common）统一捕获 `BusinessException` / `MethodArgumentNotValidException` / `Exception`
- **响应格式**：`Result<T>` 统一包装 `{code, message, data, timestamp}`
- **鉴权**：`X-User-Id` / `X-User-Name` / `X-User-Roles` header，Controller 用 `@PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")`

### 4 道闸验证（提交前必跑）
```bash
# 后端
cd solidoa && mvn compile -pl solidoa-parent/solidoa-workflow -am -DskipTests

# 前端
cd solidoa-web && npm run type-check && npm run test && npm run build
```

## 文档目录

- 📐 [SSD V2.0 优化蓝图](docs/ssd/SSD_V2.0_Optimization.md) — 19 章完整设计
- 📊 [Sprint 4 进度盘点](docs/sprint4/PROGRESS_盘点_V1.0.md) — 已完成/部分/未做清单
- 🗂 [Sprint 4 待办文档](docs/sprint4/SPRINT4_待办文档.md) — 收尾指南
- 💼 [功能设计文档](docs/requirements/) — 业务需求

## License

MIT License
