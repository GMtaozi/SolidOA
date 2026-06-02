# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SolidOA is an enterprise office automation system for **Jiangsu Dexin Gene Detection Technology Co., Ltd.**, featuring approval workflows, attendance, finance, and collaboration tools. Three application tiers share a single repository.

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

### Docker
```bash
cd D:\Project\SolidOA\solidoa\docker && docker-compose up -d
```

### Startup Script (Windows)
```
D:\Project\SolidOA\scripts\start-all.bat
```

## Architecture

### Service Map

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| solidoa-gateway | 8080 | — | API gateway, JWT auth, rate limiting, CORS |
| solidoa-system | 8081 | oa_system | Users, departments, roles, permissions, contacts, schedules, stamps |
| solidoa-workflow | 8082 | oa_workflow | Approval flows: leave, expense, purchase, stamp usage |
| solidoa-hr | 8085 | oa_hr | Attendance (DingTalk sync), finance, salary, overtime, business trips |
| solidoa-file | 8086 | oa_file | File upload/download via MinIO |
| solidoa-dingtalk | 8087 | oa_dingtalk | DingTalk enterprise integration |

### Inter-Service Communication

- **Feign clients** defined in `solidoa-common/src/main/java/com/solidoa/common/client/` (SystemClient, WorkflowClient, HrClient, MessageClient)
- Nacos service discovery is **disabled**; services use direct Feign URLs configured in each `application.yml`
- **Frontend dev proxy** (vite.config.js) routes directly to individual services, bypassing the gateway

### Module Structure (solidoa-parent)

```
solidoa-common    → Shared: entities, DTOs, Feign clients, security, exception handling, Result<T>
solidoa-gateway   → Spring Cloud Gateway routing
solidoa-system    → System management + collaboration (contacts, messages, calendar)
solidoa-workflow  → Approval workflows
solidoa-hr        → Attendance + Finance combined service
solidoa-file      → File service with MinIO
solidoa-dingtalk  → DingTalk integration
```

## Code Patterns

### Backend Layer Structure (per service)
```
controller/  → @RestController, @PreAuthorize for auth
service/     → Interface + Impl
mapper/      → MyBatis-Plus BaseMapper interfaces
entity/      → @TableName annotated, soft delete via @TableLogic
form/        → Request DTOs (validation with Jakarta)
vo/          → Response DTOs
```

### Response Format
All APIs return `Result<T>` from `com.solidoa.common.result.Result`:
```java
return Result.success(data);       // 200 + data
return Result.fail(400, "message"); // error
```

### Authentication Flow
1. Gateway validates JWT and forwards `X-User-Id`, `X-User-Name`, `X-User-Roles` headers
2. `XUserIdAuthenticationFilter` in each service sets Spring Security context from these headers
3. `@PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")` controls endpoint access

### MyBatis-Plus Conventions
- Soft delete: `@TableField(fill = FieldFill.INSERT)` on `deleted` field, `@TableLogic` annotation
- Non-persistent fields: `@TableField(exist = false)`
- Column mapping: automatic snake_case → camelCase

### Frontend API Pattern
```javascript
// src/api/system.js, workflow.js, hr.js
export const xxxApi = {
  getList: (params) => request.get('/v1/xxx', { params }),
  create: (data) => request.post('/v1/xxx', data),
  // ...
}
```

Response unwrapping in components:
```javascript
const res = await systemApi.getUserList(params)
tableData.value = res.data?.data?.records || res.data?.data || []
//              axios response → Result object → PageVO → records
```

## Database

### Five Separate Databases
- `oa_system` — sys_user, sys_department, sys_role, sys_permission, sys_user_role, sys_role_permission, sys_dict, sys_stamp
- `oa_workflow` — oa_leave, oa_expense, oa_purchase, oa_stamp, oa_approval_record, oa_approval_flow_config, oa_approval_node
- `oa_hr` — oa_salary, attendance tables, finance tables
- `oa_file` — file metadata
- `oa_dingtalk` — DingTalk sync tables

### MySQL Access
```
Host: localhost:3307 (Docker)
User: root / 749958714
Charset: utf8mb4
```

**Important**: When inserting Chinese data via `docker exec`, always use `--default-character-set=utf8mb4` and `SET NAMES utf8mb4` to avoid garbled text.

### SQL Initialization
Scripts in `solidoa/sql/init/` are auto loaded by Docker on first startup. Update scripts go in `solidoa/sql/update/`.

## Frontend Design System

Key tokens (from DESIGN.md):

| Token | Value | Usage |
|-------|-------|-------|
| Primary | `#60A5FA` | Main accent (cloud blue) |
| Success | `#34D399` | Approve/pass (mint green) |
| Warning | `#FBBF24` | Pending (honey yellow) |
| BG | `#f7f5f2` | Page background |
| Text | `#3B3B3B` | Main text |
| Border | `#F0EDE9` | Dividers |

Component specs: Cards `border-radius: 16px`, Buttons `border-radius: 12px`, Inputs `border-radius: 8px`.

## Key Constraints

1. **Cross-database JOINs not supported** — each service queries only its own database; use Feign for cross-service data
2. **Service config protection** — do not modify `server.port`, `spring.datasource.url`, gateway routes, or Feign URLs without reason
3. **No service discovery** — all inter-service calls use hardcoded URLs in application.yml
4. **Attendance from DingTalk only** — no built-in clock-in; attendance data synced from DingTalk
5. **Chinese data in MySQL** — always use `SET NAMES utf8mb4` when inserting via command line

---

## Developer Quick-Start (Sprint 1-4 实战沉淀)

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

### Frontend Design System (src/styles/)

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

**注意**：tokens.scss 与 mixins.scss 内部**不可互相 @use**（会模块循环）。tokens 自身保持纯变量定义，mixins 内直接写值（已在 Sprint 2 阶段 1 验证）。

### OaXxx 公共组件库 (src/components/)

5 个 MVP + Sprint 3 阶段 6 新增共 11 个组件：

| 组件 | 路径 | 用途 |
|------|------|------|
| **OaButton** | `OaButton/index.vue` | variant: primary/ghost/danger/default |
| **OaCard** | `OaCard/index.vue` | 容器卡片，title/hoverable/padded slots |
| **OaDialog** | `OaDialog/index.vue` | v-model 双向绑定弹窗，包装 el-dialog |
| **OaStatusBadge** | `OaStatusBadge/index.vue` | 6 态状态徽章，status-badge 替代旧 `.status-badge` 自绘 |
| **OaTable** | `OaTable/index.vue` | 表格 + 分页 + actions slot，替代原生 `<table class="data-table">` |
| **OaApprovalCard** | `OaApprovalCard/index.vue` | 审批流程卡片（单业务） |
| **OaApprovalFlow** | `OaApprovalFlow/index.vue` | 水平节点流程图（带连线+脉冲动画） |

注册方式：`main.js` 中 `app.use(GlobalComponents)`（`src/components/index.js` Vue 插件）。

### Approval State Machine (后端核心)

**位置**：`solidoa-workflow/src/main/java/com/solidoa/workflow/`

```
statemachine/
├── ApprovalStateMachine.java          # 轻量门面（链式 API）
├── ApprovalStateMachineConfig.java    # 单例配置中心（13 transitions）
├── ApprovalContext.java               # 触发上下文
└── ApprovalAction.java                # 动作接口（FunctionalInterface）

enums/
├── ApprovalState.java   # 9 状态： DRAFT/PENDING/APPROVING/APPROVED/REJECTED/WITHDRAWN/CC/FINISHED/ADD_SIGNING
└── ApprovalEvent.java    # 8 事件： SUBMIT/APPROVE/REJECT/WITHDRAW/TRANSFER/ADD_SIGN/TIMEOUT/COMPLETE

service/
├── UniversalApprovalService.java        # A1 重构：4 业务统一门卫
└── impl/UniversalApprovalServiceImpl.java
```

**关键端点**（`/api/v1/workflow/approval/{type}/{id}/...`）：
- POST `/approve` `reject` `withdraw` `transfer` `add-sign` — 状态机保护
- GET `/flow-graph` — 节点 + 边 + 抄送（前端 OaApprovalCard 数据源）
- GET `/state-machine/info` — 调试
- POST `/nodes/create` — hr 端 Feign 远程写节点（Sprint 3.4 修复 hr 5 业务缺节点）

**业务类型常量**（4 业务 + hr 端 5 业务）：LEAVE / EXPENSE / PURCHASE / STAMP / OVERTIME / BUSINESS TRIP / REPAIR_CARD / GO_OUT / SALARY

### 跨服务节点同步（Sprint 3.4 修复）

`oa_hr` 库的 5 业务（REPAIR_CARD / OVERTIME / BUSINESS_TRIP / GO_OUT / EXPENSE）**业务表在 hr 库，但流程节点需写入 `oa_workflow.oa_approval_node`**。

模式：hr 端每个 `*ServiceImpl.create*()` 末尾注入 `WorkflowClient.createApprovalNodes(type, id, applicantId)` Feign 远程调用 workflow 端 `/approval/nodes/create` 端点。

对应 `solidoa-hr/src/main/resources/application.yml`：
```yaml
feign:
  client:
    config:
      workflow-service:
        url: http://localhost:8082
```

且 `HrApplication` 已加 `@EnableFeignClients(basePackages = "com.solidoa.common.client")`。

### WorkflowClient 端点清单

```java
@FeignClient(name = "workflow-service", path = "/api/v1/workflow", configuration = FeignConfig.class)
public interface WorkflowClient {
    @GetMapping("/leave/{id}") Result<LeaveDTO> getLeaveById(@PathVariable("id") Long id);
    @GetMapping("/expense/{id}") Result<ExpenseDTO> getExpenseById(@PathVariable("id") Long id);
    @GetMapping("/attendance/{id}") Result<LeaveDTO> getAttendanceById(@PathVariable("id") Long id);
    @GetMapping("/leave/simple/{id}") Result<LeaveDTO> getLeaveSimple(@PathVariable("id") Long id);
    @PostMapping("/approval/nodes/create")
    Result<Void> createApprovalNodes(
        @RequestParam("businessType") String businessType,
        @RequestParam("businessId") Long businessId,
        @RequestParam("applicantId") Long applicantId);
}
```

### Sprint 4 已知遗留（按严重性）

🔴 严重：
- A1 第二步：4 业务 ServiceImpl 旧 if-else 仍并存，需 @Deprecated 标注 + 委托 UniversalApproval
- B1 真实推送：3 处 TODO 仍为占位日志，缺 WebSocket/钉钉真实集成
- D1 SQL 需你授权执行（V4.0__message_outbox_table.sql）

🟠 重要：参见 `docs/sprint4/SPRINT4_待办文档.md`

### 关键文件参考

- `docs/ssd/SSD_V2.0_Optimization.md` — 18 章完整优化蓝图（设计系统/组件库/工程化/状态机）
- `docs/sprint4/SPRINT4_待办文档.md` — Sprint 4 接续指南
- `solidoa-web/src/styles/tokens.scss` — 唯一设计 token 真理源
- `solidoa-web/src/components/index.js` — 11 个 OaXxx 组件统一导出
