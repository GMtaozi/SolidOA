# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SolidOA is an enterprise office automation system for **Jiangsu Dexin Gene Detection Technology Co., Ltd.**, featuring approval workflows, attendance, finance, and collaboration tools. Three application tiers share a single repository.

- **Backend**: 7 Java microservices (Spring Cloud Alibaba 2023 + Java 17)
- **Frontend**: Vue 3 + Vite + TypeScript (port 3000)
- **Mobile**: UniApp H5

**V2.0 核心改动**（与旧版本本质区别）：
- **自研 ApprovalStateMachine 状态机**替代 Camunda 重量级引擎（9 状态 / 8 事件 / 13 transitions）
- **4 业务 + 5 HR 业务** 统一委托 `UniversalApprovalService`（A1 第二步完成）
- **Sentinel + Seata + Prometheus + WebSocket + TraceId** 全部集成（V2.0 第 1/9/15 章完成）
- **16 个 OaXxx 公共组件**（V1.x 11 个 + V2.0 加 5 个）
- **删除 .github/workflows CI**（项目判断不需要，按本地 4 道闸验证）

完整状态见 `docs/sprint4/PROGRESS_盘点_V1.0.md`。

## Build Commands

### Backend (Java 17, Maven)
```bash
cd D:\Project\SolidOA\solidoa
mvn clean package -DskipTests                          # Full build
mvn clean package -DskipTests -pl solidoa-parent/solidoa-system -am  # Single module
```

### Frontend (solidoa-web)
```bash
cd D:\Project\SolidOA\solidoa-web
npm install && npm run dev      # Dev server on port 3000
npm run build                   # Production build
```

### Mobile (solidoa-app)
```bash
cd D:\Project\SolidOA\solidoa-app
npm install && npm run dev:h5   # H5 dev mode
npm run build:h5                # H5 build
```

### Docker (推荐部署方式 — 15 个容器)
```bash
cd D:\Project\SolidOA\solidoa\docker && docker-compose up -d
# MySQL + Redis + Nacos + RabbitMQ + MinIO + 6 Java 服务 + Seata + Sentinel + Prometheus + Grafana
```

## Architecture

### Service Map (8 业务 + 5 基础设施 = 13 容器)

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| solidoa-gateway | 8080 | — | API gateway, X-User-Id header 鉴权, Sentinel 限流, TraceId 注入 |
| solidoa-system | 8081 | oa_system | Users/roles/depts/permissions/dict/operlog/contacts/messages/schedules |
| solidoa-workflow | 8082 | oa_workflow | 9 业务审批流 (LEAVE/EXPENSE/STAMP/PURCHASE + 5 HR 业务) |
| solidoa-hr | 8085 | oa_hr | 考勤（钉钉同步）+ 财务/工资（合并服务） |
| solidoa-file | 8086 | oa_file | 文件上传/下载 via MinIO |
| solidoa-dingtalk | 8087 | oa_dingtalk | 钉钉集成（考勤同步） |
| seata-server | 8091 | — | 分布式事务 TC（V2.0 新增） |
| sentinel-dashboard | 8088 | — | 限流熔断控制台（V2.0 新增） |
| prometheus | 9090 | — | 指标采集（V2.0 新增） |
| grafana | 3001 | — | 可视化（V2.0 新增） |

### Inter-Service Communication

- **Feign clients** in `solidoa-common/src/main/java/com/solidoa/common/client/` (SystemClient, WorkflowClient, HrClient, MessageClient)
- Nacos 1.x 直接连（`localhost:8848`），**未启用**服务发现；服务用 Feign URL 直连
- 业务侧使用：**`@EnableFeignClients(basePackages = "com.solidoa.common.client")`** + 各服务 application.yml 配 feign.client.config.{name}.url
- **Frontend dev proxy** (vite.config.js) 路由到各服务，绕过 gateway（仅 dev 模式）

### Module Structure (solidoa-parent)
```
solidoa-common    → 共享: 实体/DTO/Feign clients/security/exception/Result<T>/TraceId 4 件套/Redis+CacheManager
solidoa-gateway   → Spring Cloud Gateway routing
solidoa-system    → 系统管理 + 协作（通讯录/消息/日程/WebSocket 推送）
solidoa-workflow  → 审批流引擎（UniversalApproval + 自研状态机）
solidoa-hr        → 考勤 + 财务（合并）
solidoa-file      → MinIO
solidoa-dingtalk  → 钉钉
```

## Code Patterns

### Backend Layer Structure (per service)
```
controller/  → @RestController, @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('USER_CREATE')")
service/     → Interface + Impl（委托 UniversalApprovalService 走状态机）
mapper/      → MyBatis-Plus BaseMapper interfaces
entity/      → @TableName + @Version 乐观锁 + @TableLogic 软删
form/        → 请求 DTOs (Jakarta validation)
vo/          → 响应 DTOs
statemachine/ → 自研状态机 (workflow 模块独有)
```

### Response Format
所有 API 返回 `Result<T>` from `com.solidoa.common.result.Result`:
```java
return Result.success(data);        // 200 + data
return Result.fail(400, "message"); // 错误
```

### Authentication Flow
1. **Gateway** 不做 JWT 校验，只注入 `X-User-Id` / `X-User-Name` / `X-User-Roles` 头（从 JWT claim 解析后转发）
2. **下游服务** `XUserIdAuthenticationFilter` 从 header 读 userId 写 Spring Security 上下文
3. **`@PreAuthorize`** 控制端点权限：
   - `isAuthenticated()` 任何登录用户
   - `hasAnyRole('ADMIN','SYSTEM_ADMIN')` 角色级
   - `hasAuthority('USER_CREATE')` 按钮级（21 个权限种子在 V2.0__permission_upgrade.sql）

### MyBatis-Plus Conventions
- **乐观锁**：`@Version` 字段 + `updateById` 走 MyBatis-Plus 自动 `WHERE version = ?` 守卫
- **软删除**：`@TableLogic` on `deleted` 字段
- **非持久字段**：`@TableField(exist = false)`
- **列映射**：自动 snake_case ↔ camelCase

### 4 道闸验证（提交前必跑）

**后端**：
```bash
cd D:\Project\SolidOA\solidoa
mvn compile -pl solidoa-parent/solidoa-workflow -am -DskipTests  # 单模块
mvn compile -DskipTests  # 全模块
```

**前端**：
```bash
cd D:\Project\SolidOA\solidoa-web
npm run type-check  # vue-tsc
npm run lint        # ESLint (0 errors, 197 historical warnings tolerated)
npm run test        # Vitest (5 文件 28 测试)
npm run build       # 含 vue-tsc 强类型检查
```

## Database

### 5 Separate Databases
- `oa_system` — sys_user, sys_role, sys_permission, sys_dict, sys_stamp, sys_oper_log, oa_message, oa_contact, oa_schedule
- `oa_workflow` — oa_leave, oa_expense, oa_purchase, oa_stamp, oa_approval_record, oa_approval_node, wf_message_outbox
- `oa_hr` — 考勤 + 财务（合并库）
- `oa_file` — 文件元数据
- `oa_dingtalk` — 钉钉同步

### MySQL Access
```
Host: localhost:3307 (Docker)
User: root / 749958714
Charset: utf8mb4 (全库全表 utf8mb4_unicode_ci)
```

**⚠️ 重要**：通过 `docker exec` 插入中文时**必须**加 `--default-character-set=utf8mb4` 并在 SQL 头加 `SET NAMES utf8mb4` 防乱码。

### SQL 命名规范 (Flyway)
- `solidoa/sql/init/` — 初始化脚本（Docker 首次启动自动跑）
- `solidoa/sql/update/V{version}__{description}.sql` — 增量更新
- 命名格式: `V1.7__stamp_and_salary_update.sql` / `V2.0__permission_upgrade.sql` / `V4.0__message_outbox_table.sql`

## Frontend Design System

`tokens.scss` 是 design token 的**唯一真理源**——颜色/字号/圆角/阴影/间距 SCSS 变量 + 双层 CSS 变量。修改全站主题色只改这里。

- `tokens.scss`（60+ 变量）：SCSS 变量 + `:root` CSS 变量双层导出
- `element-overrides.scss`（11 行）：EP `--el-color-primary` 覆盖
- `mixins.scss`（9 mixin）：card-base / btn-primary / btn-ghost / focus-glow / scrollbar / truncate / ellipsis / flex / status-badge
- `reset.scss`：替代 App.vue 内联 6 行 reset

### Vite SCSS Auto-Inject (vite.config.js)

```js
css: {
  preprocessorOptions: {
    scss: {
      additionalData: `@use "@/styles/tokens.scss" as *;\n@use "@/styles/mixins.scss" as *;\n`
    }
  }
}
```

**⚠️ 注意**：tokens.scss 与 mixins.scss 内部**不可互相 @use**（会模块循环）。tokens 自身保持纯变量定义，mixins 内直接写值（已在 Sprint 2 阶段 1 验证）。

**`__dirname` 不可用**：`vite.config.js` 是 ESM，必须用 `fileURLToPath(new URL('./src', import.meta.url))`（2026-06-02 修复，Linux runner 必需）。

### 16 个 OaXxx 公共组件 (`src/components/`)

| 组件 | 用途 |
|------|------|
| **OaButton** | variant: primary/ghost/danger/default |
| **OaCard** | 容器卡片，title/hoverable/padded slots |
| **OaDialog** | v-model 双向绑定弹窗，包装 el-dialog |
| **OaFormDialog** | V2.0 新增：OaDialog + el-form + 验证 + 提交集成 |
| **OaStatusBadge** | 6 态状态徽章 |
| **OaTable** | 表格 + 分页 + actions slot |
| **OaApprovalCard** | 审批流程卡片 |
| **OaApprovalFlow** | 水平节点流程图（带连线+脉冲动画，含 currentNodeOrder 高亮）|
| **OaEmpty** | V2.0 新增：空状态 |
| **OaPagination** | V2.0 新增：分页 v-model:page + v-model:size |
| **OaSearchForm** | V2.0 新增：查询表单 + 重置 |
| **OaPageHeader** | V2.0 新增：页面标题栏 |
| **OaIcon** | V2.0 新增：Element Plus 图标包装（按名字取）|

注册方式：`main.js` 中 `app.use(GlobalComponents)`（`src/components/index.js` Vue 插件）。

**⚠️ SASS 变量名注意**：tokens.scss 用 `$primary / $success`（无前缀）。**不要**用 `$text-1 / $bg-card / $border`（这些不存在，会 build 失败）。新组件 SCSS 里**直接写十六进制值**或**用 mixin**，不要 @use tokens 模块。

### API Pattern
```javascript
// solidoa-web/src/api/system.ts, workflow.ts, hr.ts
export const xxxApi = {
  getList: (params) => request.get('/v1/xxx', { params }),
  create: (data) => request.post('/v1/xxx', data),
}
```

Response unwrapping:
```javascript
const res = await systemApi.getUserList(params)
tableData.value = res.data?.data?.records || res.data?.data || []
// axios response → Result {code, message, data} → PageVO {records, total, ...}
```

## V2.0 核心架构（与 V1.x 区别）

### Approval State Machine (后端核心 — 替代 Camunda)

**位置**：`solidoa-workflow/src/main/java/com/solidoa/workflow/statemachine/`

```
ApprovalStateMachine.java          # 轻量门面（链式 API）
ApprovalStateMachineConfig.java    # 单例配置中心（13 transitions）
ApprovalContext.java               # 触发上下文（userId/comment/payload）
ApprovalAction.java                # 动作接口（FunctionalInterface）
UniversalApprovalService.java       # A1 重构：4 业务 + 5 HR 业务统一门卫
UniversalApprovalServiceImpl.java  # 实现：状态机 + 乐观锁守卫
```

- **9 状态**：`DRAFT / PENDING / APPROVING / APPROVED / REJECTED / WITHDRAWN / CC / FINISHED / ADD_SIGNING`
- **8 事件**：`SUBMIT / APPROVE / REJECT / WITHDRAW / TRANSFER / ADD_SIGN / TIMEOUT / COMPLETE`
- **13 transitions** 在 `ApprovalStateMachineConfig.getInstance()`

**关键端点** (`/api/v1/workflow/approval/{type}/{id}/...`)：
- POST `/approve` `/reject` `/withdraw` `/transfer` `/add-sign` — 状态机保护
- GET `/flow-graph` — 节点 + 边 + 抄送 + currentNodeOrder（前端 OaApprovalFlow 数据源）
- GET `/timeline` — 顺序时间线（V2.0 新增）
- GET `/state-machine/info` — 调试
- POST `/nodes/create` — hr 端 Feign 远程写节点

### UniversalApprovalService — 4 业务 ServiceImpl 委托模式

**位置**：`solidoa-workflow/src/main/java/com/solidoa/workflow/service/UniversalApprovalServiceImpl.java`

```java
public ApprovalState fire(String businessType, Long businessId, Long userId,
                          ApprovalEvent event, String comment) {
    // 1. 读业务表状态
    // 2. 构造 ApprovalContext
    // 3. ApprovalStateMachine.canFire() 守卫
    // 4. fire 触发状态机
    // 5. updateById 同步业务表（MyBatis-Plus @Version 自动乐观锁）
}
```

**4 业务（Leave/Expense/Stamp/Purchase）+ 5 HR 业务（Overtime/BusinessTrip/RepairCard/GoOut/Salary）** 全部委托此入口，业务副作用（写 ApprovalRecord / 发消息）保留在 ServiceImpl 内。

### 跨服务节点同步（hr 端 5 业务）

`oa_hr` 库的 5 业务**业务表在 hr 库，但流程节点需写入 `oa_workflow.oa_approval_node`**。

模式：hr 端每个 `*ServiceImpl.create*()` 末尾注入 `WorkflowClient.createApprovalNodes(type, id, applicantId)` Feign 调用。

### CacheManager 5 域（V2.0 第 15 章）

**位置**：`solidoa-common/src/main/java/com/solidoa/common/config/RedisConfig.java`

```java
RedisCacheManager.builder(factory)
    .withCacheConfiguration("oa:user",     config.entryTtl(Duration.ofHours(4)))
    .withCacheConfiguration("oa:dept",     config.entryTtl(Duration.ofHours(8)))
    .withCacheConfiguration("oa:dict",     config.entryTtl(Duration.ofHours(24)))
    .withCacheConfiguration("oa:contact",  config.entryTtl(Duration.ofMinutes(30)))
    .withCacheConfiguration("oa:schedule", config.entryTtl(Duration.ofMinutes(10)))
    .withCacheConfiguration("oa:dashboard",config.entryTtl(Duration.ofMinutes(5)))
    .build();
```

**安全规则**：@Cacheable key **必须含 userId/deptId 等身份字段**，避免越权。
- ✅ `key="#userId + ':' + #startDate + ':' + #endDate"`
- ❌ `key="#id"` 单独 ID（不同用户可能查到别人的数据）

### TraceId 全链路（V2.0 第 15 章）

4 件套：
1. **Gateway** `TraceIdFilter`（已有）— 生成/读取 `X-Trace-Id` 头
2. **下游** `TraceIdInterceptor`（solidoa-common）— 接收头写入 MDC
3. **Feign** `TraceIdFeignInterceptor`（solidoa-common）— 调下游时透传
4. **logback pattern** — `[%X{traceId}]` 已在所有 logback-spring.xml

### WebSocket 实时推送（V2.0 第 9 章 MVP）

- 端点：`/ws/notification`（system 服务）
- SessionManager：`ConcurrentHashMap<Long, WebSocketSession>` in-memory
- 推进模式：MVP 单进程，**未接 Redis pub/sub**（明天完善）
- 依赖：system pom 已有 `spring-boot-starter-websocket`

### Sentinel 限流（V2.0 第 1 章）

- workflow 集成 `spring-cloud-starter-alibaba-sentinel`
- 6 条规则在 `solidoa/docker/sentinel/flow-rules.json`
- application.yml 配 `spring.cloud.sentinel.transport.dashboard=localhost:8088`
- Feign 限流自动启用（`feign.sentinel.enabled: true`）

### Seata 分布式事务（V2.0 第 1 章）

- seata-server 容器（端口 8091）+ Nacos 注册中心
- 业务侧 `@GlobalTransactional` **未集成**（待 V2.0 第 1 章续，缺 BalanceService/BudgetService）

### Prometheus + Grafana

- 每个 Java 服务 application.yml 暴露 `/actuator/prometheus`
- prometheus.yml 抓 5 个 solidoa 服务 + 自身
- grafana 配置 prometheus 数据源（默认账号 admin/admin）

## Key Constraints

1. **跨库 JOIN 不支持** — 每个服务只查自己的库，跨库用 Feign
2. **服务配置保护** — 不要无理由修改 `server.port` / `spring.datasource.url` / Feign URL
3. **Nacos 服务发现未启用** — Feign URL 硬编码在 application.yml
4. **考勤仅从钉钉同步** — 无内置打卡
5. **中文数据 MySQL** — 必须 `SET NAMES utf8mb4`
6. **不要自己加 CI** — 已删除 `.github/workflows/`，按本地 4 道闸验证

## 关键文件参考

- `solidoa/README.md` — 项目主文档（V2.0 重写版）
- `docs/ssd/SSD_V2.0_Optimization.md` — 19 章优化蓝图
- `docs/sprint4/PROGRESS_盘点_V1.0.md` — Sprint 4-5 完成度盘点（明天接续入口）
- `docs/sprint4/SPRINT4_待办文档.md` — Sprint 4 收尾指南
- `solidoa-web/src/styles/tokens.scss` — 唯一设计 token 真理源
- `solidoa-web/src/components/index.js` — 16 个 OaXxx 组件统一导出
- `solidoa-common/.../RedisConfig.java` — CacheManager 5 域
- `solidoa-common/.../interceptor/TraceIdInterceptor.java` — 下游 TraceId
