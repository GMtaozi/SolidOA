# SolidOA 系统规格设计文档（SSD V2.0）

> 版本：V2.0（优化重设计版）
> 日期：2026-06-02
> 作者：SolidOA Architecture Team
> 状态：设计评审稿
> 前置文档：V1.0/V1.1/V1.3、探索报告（2026-06-02）、优化蓝图

---

## 文档说明

本文档基于 2026-06-02 的代码全局探索报告与泛微 e-cology 对标分析，**对 V1.x 已有模块的优化方向 + 缺失模块的全新设计**进行完整规约。

每章明确标注 4 种处理类型：

| 标记          | 含义                  | 动作        |
| ----------- | ------------------- | --------- |
| 🟢 **保持**   | V1.x 设计合理，仅做局部微调    | 1-2 天内可完成 |
| 🟡 **重构**   | V1.x 架构存在显著问题，需重新设计 | 3-10 天工作量 |
| 🔴 **全新设计** | 当前完全缺失，需从零设计        | 5-15 天工作量 |
| 🟠 **扩展**   | V1.x 已有基础，需追加能力     | 2-5 天工作量  |

---

## 目录

1. [总体架构重新设计](#1-总体架构重新设计) 🟡 重构
2. [设计系统规范](#2-设计系统规范) 🔴 全新设计
3. [通用组件库规范](#3-通用组件库规范) 🔴 全新设计
4. [工程化体系（TS/ESLint/Prettier/测试）](#4-工程化体系) 🔴 全新设计
5. [系统管理模块优化](#5-系统管理模块优化) 🟡 重构
6. [审批流引擎重构（State Machine）](#6-审批流引擎重构) 🟡 重构
7. [考勤模块扩展](#7-考勤模块扩展) 🟢 保持 + 🟠 扩展
8. [财务/工资模块补齐](#8-财务工资模块补齐) 🟠 扩展
9. [通讯录/日程/消息补齐](#9-通讯录日程消息补齐) 🟡 重构
10. [用印模块去重](#10-用印模块去重) 🟡 重构
11. [公告通知模块](#11-公告通知模块) 🔴 全新设计
12. [资产管理模块](#12-资产管理模块) 🔴 全新设计
13. [知识文档模块](#13-知识文档模块) 🔴 全新设计
16. [钉钉集成重构](#16-钉钉集成重构) 🟡 重构
17. [缓存层与可观测性](#17-缓存层与可观测性) 🟠 扩展
18. [数据库规范升级](#18-数据库规范升级) 🟠 扩展
19. [部署与运维规范](#19-部署与运维规范) 🟢 保持 + 🟠 扩展
20. [实施路线图与验收标准](#20-实施路线图)

---

## 1. 总体架构重新设计 🟡 重构

### 1.1 现状问题

| 问题                               | 证据                                                                           |
| -------------------------------- | ---------------------------------------------------------------------------- |
| solidoa-common 存在两份              | 顶层 `solidoa-common/`（4 文件，裁剪版）vs `solidoa-parent/solidoa-common/`（71 文件，完整版） |
| solidoa-dingtalk 存在两份            | 顶层 `solidoa-dingtalk/`（27 文件，旧版）vs `solidoa-parent/` 下空壳                     |
| Stamp 模块在 system 与 workflow 各有一份 | 两套实体 + 两个 Controller                                                         |
| Message 模块完全空                    | `system/message/` 5 个子目录全空                                                   |
| 分布式事务缺失                          | 全部单服务 `@Transactional`                                                       |
| 缓存层未抽象                           | 仅 3 处 RedisTemplate 调用，无 `@Cacheable`                                        |
| traceId 未透传                      | gateway 有 `TraceIdFilter`，下游无 MDC 注入                                         |
| Sentinel 规则未配置                   | 依赖开启但无 dashboard 接入                                                          |

### 1.2 目标架构

```
D:\Project\SolidOA\
├── solidoa-parent/                    # Maven 聚合根（唯一）
│   ├── pom.xml
│   ├── solidoa-common/                # 共享库（唯一）
│   ├── solidoa-gateway/               # 8080 API 网关
│   ├── solidoa-system/                # 8081 系统 + 协作 + 通讯录
│   ├── solidoa-workflow/              # 8082 审批引擎（合并用印）
│   ├── solidoa-hr/                    # 8085 考勤 + 财务
│   ├── solidoa-file/                  # 8086 文件
│   ├── solidoa-dingtalk/              # 8087 钉钉（迁移并整合）
│   ├── solidoa-notice/                # 8083 公告通知（新增）
│   ├── solidoa-asset/                 # 8088 资产管理（新增）
│   ├── solidoa-knowledge/             # 8089 知识文档（新增）
│   └── solidoa-collaboration/         # 8090 通讯录/日程/消息（从 system 拆分）
│
├── solidoa-web/                       # 前端（Vue3 + TS）
├── solidoa-app/                       # 移动端 H5
├── solidoa/                           # ⚠️ 待删除（旧根目录，sql/init 保留）
├── docker/                            # Docker Compose
├── docs/                              # 设计文档
└── scripts/                           # 部署脚本
```

### 1.3 服务拆分原则

| 服务                        | 拆分理由                     | 数据库          |
| ------------------------- | ------------------------ | ------------ |
| **solidoa-collaboration** | 通讯录/日程/消息访问量高，独立部署不影响核心  | oa_collab    |
| **solidoa-notice**        | 公告推送全网广播，独立保证 SLA        | oa_notice    |
| **solidoa-asset**         | 资产管理独立，与人事解耦              | oa_asset     |
| **solidoa-knowledge**     | 文档存储量大，独立数据库便于分库分表       | oa_knowledge |

### 1.4 跨服务调用规范

```yaml
# application.yml 统一配置（每个服务）
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: BASIC
        requestInterceptors:
          - com.solidoa.common.feign.TraceIdFeignInterceptor
  sentinel:
    enabled: true
    transport:
      dashboard: localhost:8080  # Sentinel Dashboard

# 统一 Sentinel 规则（独立配置文件）
spring:
  cloud:
    sentinel:
      rules:
        - resource: workflow-approve
          grade: QPS
          count: 100
          controlBehavior: RejectFast
```

### 1.5 数据一致性方案

| 场景      | 方案                      | 说明                                    |
| ------- | ----------------------- | ------------------------------------- |
| 单服务内多表写 | `@Transactional` 本地事务   | 不变                                    |
| 跨服务异步通知 | **本地消息表 + MQ**          | 沿用 `WorkflowMessageProducer/Consumer` |
| 跨服务强一致  | 引入 **Seata AT 模式**      | 仅用于核心链路：请假扣减余额、报销冻结预算                 |
| 幂等      | **Redis SETNX + Token** | 沿用 `IdempotentService`                |
| 分布式锁    | **Redisson**            | 新增，用于资产领用冲突                            |

### 1.6 验收标准

- [ ] 顶层 `solidoa-common/`、`solidoa-dingtalk/` 旧目录删除
- [ ] Maven 聚合根只剩 `solidoa-parent/pom.xml`
- [ ] 9 个服务（含 5 个新增）能独立打包部署
- [ ] Feign 调用全部走 Sentinel dashboard
- [ ] 至少 1 条核心链路接入 Seata

---

## 2. 设计系统规范 🔴 全新设计

### 2.1 现状问题

- `DESIGN.md` 定义完整 token 但**未被代码引用**
- `src/styles/variables.scss` 仍是 Element Plus 默认色板
- 34 个页面在 `<style scoped>` 里重复声明 `$primary`

### 2.2 Token 体系（CSS 变量 + SCSS 变量双层）

```scss
// src/styles/tokens.scss
:root {
  // === 颜色（来自 DESIGN.md） ===
  --color-primary: #60A5FA;       // 云蓝（主色）
  --color-primary-hover: #3B82F6;
  --color-primary-active: #2563EB;
  --color-primary-bg: #EFF6FF;

  --color-success: #34D399;       // 薄荷绿
  --color-warning: #FBBF24;       // 蜜糖黄
  --color-danger:  #FCA5A5;       // 桃红
  --color-info:    #94A3B8;       // 雾灰

  // 中性色
  --color-bg:        #f7f5f2;     // 页面背景
  --color-bg-card:   #FFFFFF;
  --color-bg-hover:  #FAFAFA;
  --color-text:      #3B3B3B;
  --color-text-secondary: #6B7280;
  --color-text-tertiary:  #9CA3AF;
  --color-border:    #F0EDE9;
  --color-divider:   #E5E7EB;

  // === 圆角（来自 DESIGN.md） ===
  --radius-sm:  6px;              // Tag
  --radius-md:  8px;              // Input
  --radius-lg: 12px;              // Button
  --radius-xl: 16px;              // Card / Dialog
  --radius-full: 9999px;          // 头像

  // === 间距（8 栅格） ===
  --space-1:  4px;
  --space-2:  8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;

  // === 阴影 ===
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 4px 12px rgba(15, 23, 42, 0.06);
  --shadow-lg: 0 10px 25px rgba(15, 23, 42, 0.08);
  --shadow-xl: 0 20px 40px rgba(15, 23, 42, 0.12);

  // === 字体 ===
  --font-size-xs: 12px;
  --font-size-sm: 13px;
  --font-size-md: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 18px;
  --font-size-2xl: 20px;
  --font-size-3xl: 24px;
  --font-weight-regular: 400;
  --font-weight-medium:  500;
  --font-weight-semibold: 600;
  --font-weight-bold:    700;

  // === 动画 ===
  --motion-fast:   150ms;
  --motion-base:   250ms;
  --motion-slow:   400ms;
  --ease-standard: cubic-bezier(0.4, 0.0, 0.2, 1);
}

// SCSS 桥接（让 SCSS 代码也能用）
$primary:    var(--color-primary);
$bg:         var(--color-bg);
// ... 同步暴露
```

### 2.3 Element Plus 主题覆盖

```scss
// src/styles/element-overrides.scss
:root {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: #93C5FD;
  --el-color-primary-light-5: #BFDBFE;
  --el-color-primary-light-7: #DBEAFE;
  --el-color-primary-light-8: #EFF6FF;
  --el-color-primary-light-9: #F8FAFC;
  --el-color-primary-dark-2: var(--color-primary-active);

  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger:  var(--color-danger);
  --el-color-info:    var(--color-info);

  --el-border-radius-base: var(--radius-md);
  --el-border-radius-small: var(--radius-sm);
  --el-border-radius-round: var(--radius-full);
}
```

### 2.4 暗黑模式（可选 v2.1 落地）

```scss
[data-theme="dark"] {
  --color-bg:        #0F172A;
  --color-bg-card:   #1E293B;
  --color-text:      #F1F5F9;
  --color-text-secondary: #CBD5E1;
  --color-border:    #334155;
  // ... 其他 token 反转
}
```

### 2.5 验收标准

- [ ] 移除 `src/styles/variables.scss` 中所有 Element Plus 默认色
- [ ] 所有 `.vue` 文件 `<style scoped>` 内不出现颜色/圆角/间距硬编码
- [ ] 全局切换主色 1 处生效（修改 `tokens.scss` 一行即可全站变）
- [ ] DESIGN.md 与 `tokens.scss` 字段一一对应

---

## 3. 通用组件库规范 🔴 全新设计

### 3.1 组件清单（13 个核心 + 5 个业务）

| 组件                         | 类型  | 优先级 | 来源                                         |
| -------------------------- | --- | --- | ------------------------------------------ |
| `<oa-table>`               | 核心  | P0  | 抽离自 finance/expense、attendance/repair-card |
| `<oa-form-dialog>`         | 核心  | P0  | 抽离自所有 dialog 场景                            |
| `<oa-page-header>`         | 核心  | P0  | 34 个页面都缺统一头部                               |
| `<oa-empty>`               | 核心  | P0  | 各页空状态五花八门                                  |
| `<oa-status-tag>`          | 核心  | P0  | 状态色散落各页                                    |
| `<oa-search-form>`         | 核心  | P0  | 顶部查询表单                                     |
| `<oa-pagination>`          | 核心  | P0  | 抽离 el-pagination 二次封装                      |
| `<oa-loading>`             | 核心  | P1  | 统一 loading 样式                              |
| `<oa-error-boundary>`      | 核心  | P1  | 错误兜底                                       |
| `<oa-icon>`                | 核心  | P1  | 解决 33 个图标全量注册的性能问题                         |
| `<oa-avatar>`              | 核心  | P1  | 用户头像                                       |
| `<oa-tree-select>`         | 核心  | P1  | 部门/角色选择                                    |
| `<oa-upload>`              | 核心  | P1  | 基于 file.js 封装                              |
| `<oa-approval-card>`       | 业务  | P0  | 审批流时间线                                     |
| `<oa-approval-node>`       | 业务  | P0  | 审批节点展示                                     |
| `<oa-attendance-calendar>` | 业务  | P0  | 考勤日历                                       |
| `<oa-budget-bar>`          | 业务  | P1  | 预算进度条                                      |
| `<oa-message-bell>`        | 业务  | P0  | 消息铃铛                                       |

### 3.2 `<oa-table>` 详细设计

```typescript
// src/components/OaTable/index.vue
<script setup lang="ts" generic="T extends Record<string, any>">
interface Column {
  prop: keyof T
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  formatter?: (row: T) => string | VNode
  slot?: string
}

interface Props {
  columns: Column[]
  data: T[]
  total: number
  page: number
  size: number
  loading?: boolean
  rowKey?: keyof T
  selection?: boolean
  index?: boolean
  actionWidth?: number
  actions?: (row: T) => ActionItem[]
}

interface ActionItem {
  label: string
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  icon?: string
  permission?: string         // 按钮级权限
  onClick: (row: T) => void
}

// emit: update:page, update:size, refresh, selection-change, action-click
</script>
```

**使用示例**：

```vue
<oa-table
  :columns="columns"
  :data="tableData"
  :total="total"
  v-model:page="query.page"
  v-model:size="query.size"
  :loading="loading"
  :actions="getActions"
  @refresh="fetchData"
/>
```

### 3.3 `<oa-approval-card>` 详细设计

展示审批流的时间线，支持查看流程图、加签、撤回、转交等操作。

```typescript
interface ApprovalCardProps {
  flowConfig: ApprovalFlowConfig   // 流程配置（含 nodes）
  records: ApprovalRecord[]        // 审批记录
  currentNodeOrder: number         // 当前节点
  readonly?: boolean
  showActions?: boolean
}

interface ApprovalCardEmits {
  (e: 'approve', record: ApprovalRecord): void
  (e: 'reject', record: ApprovalRecord): void
  (e: 'transfer', record: ApprovalRecord): void
  (e: 'addSign', record: ApprovalRecord): void
  (e: 'withdraw'): void
}
```

### 3.4 验收标准

- [ ] 13 个核心组件全部落地，组件库可独立 npm 打包
- [ ] 至少 5 个旧页面（finance/expense、attendance/repair-card、workflow/stamp 等）重构为使用新组件
- [ ] 单文件 `.vue` 不再超过 20KB
- [ ] 组件库单元测试覆盖率 ≥ 80%

---

## 4. 工程化体系 🔴 全新设计

### 4.1 TypeScript 化（分 3 阶段）

**阶段 1：基础设施（1 天）**

```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ESNext",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "jsx": "preserve",
    "types": ["vite/client", "element-plus/global"],
    "paths": {
      "@/*": ["src/*"]
    },
    "baseUrl": "."
  },
  "include": ["src/**/*", "src/**/*.vue", "vite.config.ts"]
}
```

**阶段 2：API 层迁移（2-3 天）**

```typescript
// src/api/types.ts
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
  [key: string]: unknown
}

export interface PageVO<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface User {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  deptId: number
  deptName?: string
  status: 0 | 1
  createTime: string
  // ...
}
```

```typescript
// src/api/system.ts
import request from '@/utils/request'
import type { Result, PageQuery, PageVO, User } from './types'

export const userApi = {
  getList: (params: PageQuery & { deptId?: number }) =>
    request.get<PageVO<User>>('/v1/users', { params }),
  create: (data: Partial<User>) =>
    request.post<Result<User>>('/v1/users', data),
  update: (id: number, data: Partial<User>) =>
    request.put<Result<void>>(`/v1/users/${id}`, data),
  remove: (id: number) =>
    request.delete<Result<void>>(`/v1/users/${id}`),
  resetPassword: (id: number, newPwd: string) =>
    request.post<Result<void>>(`/v1/users/${id}/reset-password`, { newPwd }),
}
```

**阶段 3：组件 Props/Emits 类型化（持续）**

### 4.2 ESLint + Prettier

```js
// .eslintrc.cjs
module.exports = {
  root: true,
  extends: [
    'plugin:vue/vue3-recommended',
    'eslint:recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier',
  ],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  rules: {
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-unused-vars': 'warn',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
  },
}
```

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "arrowParens": "always"
}
```

### 4.3 Vitest 单测

```typescript
// src/composables/__tests__/useTable.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { useTable } from '../useTable'

describe('useTable', () => {
  it('应正确初始化分页参数', () => {
    const fetchFn = vi.fn()
    const { query, total, loading } = useTable(fetchFn)
    expect(query.value).toEqual({ page: 1, size: 10 })
    expect(total.value).toBe(0)
    expect(loading.value).toBe(false)
  })

  it('应在 reset 后回到第一页', async () => {
    const fetchFn = vi.fn().mockResolvedValue({ records: [], total: 0 })
    const { reset, query } = useTable(fetchFn)
    query.value.page = 5
    await reset()
    expect(query.value.page).toBe(1)
  })
})
```

### 4.4 验收标准

- [ ] 100% `.js` → `.ts` 迁移完成（不含第三方）
- [ ] `npm run lint` 0 errors
- [ ] `npm run test` 覆盖率 ≥ 60%（v2.0 目标），v2.1 提至 80%
- [ ] CI 流程加入 lint + type-check + test 三道闸

---

## 5. 系统管理模块优化 🟡 重构

### 5.1 用户/部门/角色（V1.x 已较好，仅扩展）

#### 5.1.1 扩展项

| 功能      | 说明                                             | 优先级 |
| ------- | ---------------------------------------------- | --- |
| 批量导入用户  | Excel 导入 + 钉钉批量同步                              | P0  |
| 管理员重置密码 | `POST /v1/users/{id}/reset-password`           | P0  |
| 离职用户禁用  | 软删除 + 关联数据归档                                   | P1  |
| 部门拖拽排序  | 前端 el-tree + 后端 sort 字段                        | P1  |
| 数据权限    | 按部门/角色过滤（行级权限）                                 | P1  |
| 按钮级权限   | `@PreAuthorize("hasAuthority('user:create')")` | P0  |

#### 5.1.2 权限模型升级

```sql
-- V2.0 新增字段
ALTER TABLE sys_permission
  ADD COLUMN menu_id BIGINT COMMENT '关联菜单ID',
  ADD COLUMN perm_type VARCHAR(20) DEFAULT 'BUTTON' COMMENT 'MENU/BUTTON/API',
  ADD COLUMN icon VARCHAR(50) COMMENT '菜单图标',
  ADD COLUMN path VARCHAR(200) COMMENT '前端路由',
  ADD COLUMN component VARCHAR(200) COMMENT '前端组件路径',
  ADD COLUMN hidden TINYINT DEFAULT 0 COMMENT '是否隐藏',
  ADD COLUMN sort INT DEFAULT 0;

-- 角色权限表增加数据权限范围
ALTER TABLE sys_role
  ADD COLUMN data_scope TINYINT DEFAULT 1
    COMMENT '1全部 2本部门 3本部门及下级 4本人 5自定义';
```

```java
// 后端实现
@DataPermission(scope = "#user.dataScope", deptField = "dept_id")
public PageVO<Expense> listExpenses(PageQuery query) { ... }
```

### 5.2 字典管理（V1.x 有，扩展）

- 新增字典缓存：启动时加载到 Redis，按 `dictType` 缓存
- 字典翻译注解：`@DictTrans(field = "status", type = "expense_status")`

### 5.3 操作日志（V1.x 有，扩展）

- 当前 `sys_oper_log` 仅记录，需要做可视化查询页面
- 异步写入（用 `@Async` + 消息队列）

### 5.4 验收标准

- [ ] 权限模型升级 SQL 在 `sql/update/V2.0/01_permission_upgrade.sql` 落地
- [ ] 至少 3 个 Controller 接入 `@PreAuthorize` 按钮级权限
- [ ] 用户/角色/部门页全部使用 `<oa-table>` + `<oa-form-dialog>`

---

## 6. 审批流引擎重构 🟡 重构

### 6.1 现状问题

- `ApprovalFlowServiceImpl` 大段 if-else 状态流转
- `ApprovalController` 仅 2 个端点，缺**撤回/加签/减签/流程图**
- 状态枚举散落，无统一状态机

### 6.2 引入 Spring State Machine

#### 6.2.1 状态定义

```java
public enum ApprovalState {
  DRAFT,            // 草稿
  PENDING,          // 待审批
  APPROVING,        // 审批中（已有人在审）
  APPROVED,         // 已通过
  REJECTED,         // 已拒绝
  WITHDRAWN,        // 已撤回
  CC,               // 抄送
  FINISHED,         // 终态
  ADD_SIGNING,      // 加签中
  TRANSFERRED       // 已转交
}
```

#### 6.2.2 事件定义

```java
public enum ApprovalEvent {
  SUBMIT,           // 提交
  APPROVE,          // 同意
  REJECT,           // 拒绝
  WITHDRAW,         // 撤回
  TRANSFER,         // 转交
  ADD_SIGN,         // 加签
  ADD_SIGN_COMPLETE,// 加签完成
  CC_READ,          // 抄送已读
  TIMEOUT           // 超时
}
```

#### 6.2.3 状态机配置

```java
@Configuration
@EnableStateMachine
public class ApprovalStateMachineConfig
    extends StateMachineConfigurerAdapter<ApprovalState, ApprovalEvent> {

  @Override
  public void configure(StateMachineStateConfigurer<ApprovalState, ApprovalEvent> states)
      throws Exception {
    states
      .withStates()
        .initial(ApprovalState.DRAFT)
        .states(EnumSet.allOf(ApprovalState.class))
        .end(ApprovalState.FINISHED)
        .end(ApprovalState.WITHDRAWN);
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<ApprovalState, ApprovalEvent> transitions)
      throws Exception {
    transitions
      .withExternal()
        .source(ApprovalState.DRAFT).target(ApprovalState.PENDING)
        .event(ApprovalEvent.SUBMIT)
        .action(auditAction(), notifyApproverAction())
      .and()
      .withExternal()
        .source(ApprovalState.PENDING).target(ApprovalState.APPROVING)
        .event(ApprovalEvent.APPROVE)
        .guard(isFirstApproverGuard())
      .and()
      .withExternal()
        .source(ApprovalState.APPROVING).target(ApprovalState.APPROVED)
        .event(ApprovalEvent.APPROVE)
        .guard(isLastApproverGuard())
        .action(finishAction())
      .and()
      .withExternal()
        .source(ApprovalState.PENDING).target(ApprovalState.REJECTED)
        .event(ApprovalEvent.REJECT)
        .action(rejectAction())
      .and()
      .withExternal()
        .source(ApprovalState.PENDING).target(ApprovalState.WITHDRAWN)
        .event(ApprovalEvent.WITHDRAW)
        .guard(isInitiatorGuard())
      // ... 更多 transition
      ;
  }
}
```

#### 6.2.4 端点扩展

| 端点                              | 方法   | 说明            |
| ------------------------------- | ---- | ------------- |
| `/v1/approvals`                 | POST | 发起审批（V1.x 已有） |
| `/v1/approvals/{id}/approve`    | POST | 同意（V1.x 已有）   |
| `/v1/approvals/{id}/reject`     | POST | 拒绝（V1.x 已有）   |
| `/v1/approvals/{id}/withdraw`   | POST | **新增**撤回      |
| `/v1/approvals/{id}/transfer`   | POST | **新增**转交      |
| `/v1/approvals/{id}/add-sign`   | POST | **新增**加签      |
| `/v1/approvals/{id}/flow-graph` | GET  | **新增**流程图     |
| `/v1/approvals/{id}/timeline`   | GET  | **新增**审批时间线   |

#### 6.2.5 流程图渲染

前端使用 `<oa-approval-card>` 组件，后端返回结构化数据：

```json
{
  "nodes": [
    { "id": "1", "name": "提交申请", "type": "START", "userId": 100, "userName": "张三", "time": "2026-06-01 09:00:00" },
    { "id": "2", "name": "直属领导审批", "type": "APPROVE", "userId": 2, "userName": "丁鑫", "time": "2026-06-01 10:30:00", "status": "APPROVED" },
    { "id": "3", "name": "财务审核", "type": "APPROVE", "userId": null, "userName": null, "time": null, "status": "PENDING" }
  ],
  "edges": [
    { "from": "1", "to": "2" },
    { "from": "2", "to": "3" }
  ],
  "currentNodeId": "3"
}
```

### 6.3 验收标准

- [ ] 状态机独立模块 `solidoa-workflow/statemachine/`
- [ ] 8 个端点全部实现并有单测
- [ ] 流程图可视化组件支持高亮当前节点
- [ ] 加签/转交/撤回覆盖至少 3 种业务类型（请假/报销/用印）的端到端测试

---

## 7. 考勤模块扩展 🟢 保持 + 🟠 扩展

### 7.1 V1.x 已成熟，保持不变

- 班次/考勤组/规则/节假日/调休/余额初始化
- 6 维度汇总（迟到/早退/旷工/请假/出差/加班）
- 补卡/外出/加班/出差流程

### 7.2 扩展项

| 功能        | 说明                     | 优先级 |
| --------- | ---------------------- | --- |
| 考勤日历视图    | 月历展示每日打卡状态             | P0  |
| 排班可视化     | 拖拽排班                   | P1  |
| 加班自动转调休   | 配置规则：加班 N 小时 = 调休 N 小时 | P1  |
| 考勤异常主动告警  | 钉钉消息推送                 | P0  |
| 弹性工作制     | 半天工作制、错峰上班             | P2  |
| 人脸识别对接    | 钉钉/海康                  | P2  |
| 考勤月报/年报导出 | Excel                  | P0  |

### 7.3 验收标准

- [ ] 考勤日历组件 `<oa-attendance-calendar>` 落地
- [ ] 异常告警走消息中心 + 钉钉双通道
- [ ] 月报导出支持 PDF + Excel

---

## 8. 财务/工资模块补齐 🟠 扩展

### 8.1 现状问题

- 缺少个税累计预扣法逻辑
- `oa_payment_log` 字段简单，未对接真实银行
- 缺财务月结/年结

### 8.2 扩展设计

#### 8.2.1 个税专项附加扣除

```sql
-- 个人专项附加扣除信息表
CREATE TABLE oa_tax_deduction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  tax_year INT NOT NULL,
  -- 6 项专项附加扣除
  children_education DECIMAL(10,2) DEFAULT 0 COMMENT '子女教育',
  continuing_education DECIMAL(10,2) DEFAULT 0 COMMENT '继续教育',
  major_illness DECIMAL(10,2) DEFAULT 0 COMMENT '大病医疗',
  housing_loan DECIMAL(10,2) DEFAULT 0 COMMENT '住房贷款利息',
  housing_rent DECIMAL(10,2) DEFAULT 0 COMMENT '住房租金',
  elder_care DECIMAL(10,2) DEFAULT 0 COMMENT '赡养老人',
  -- 其它
  social_insurance DECIMAL(10,2) DEFAULT 0 COMMENT '社保',
  housing_fund DECIMAL(10,2) DEFAULT 0 COMMENT '公积金',
  -- ...
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_year (user_id, tax_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```java
// 个税计算服务
public class TaxCalculator {
  // 累计预扣法
  public TaxResult calculateCumulative(TaxContext ctx) {
    BigDecimal cumulativeIncome = ctx.getYtdIncome();
    BigDecimal cumulativeDeduction = ctx.getYtdDeduction();
    BigDecimal cumulativeTaxable = cumulativeIncome.subtract(cumulativeDeduction).subtract(BigDecimal.valueOf(60000));  // 起征点
    if (cumulativeTaxable.compareTo(BigDecimal.ZERO) <= 0) {
      return TaxResult.zero();
    }
    BigDecimal tax = applyProgressiveRate(cumulativeTaxable);
    BigDecimal taxPaid = ctx.getYtdTaxPaid();
    BigDecimal currentTax = tax.subtract(taxPaid);
    return new TaxResult(cumulativeTaxable, tax, currentTax);
  }
}
```

#### 8.2.2 财务月结

```java
// 月结流程
public class MonthEndService {
  public void executeMonthEnd(int year, int month) {
    // 1. 锁定当月所有报销
    expenseService.lockMonth(year, month);
    // 2. 锁定当月所有工资
    salaryService.lockMonth(year, month);
    // 3. 生成会计凭证（占位，后续对接金蝶/用友）
    accountingService.generateVouchers(year, month);
    // 4. 发送月结通知
    messageService.sendMonthEndNotice(year, month);
  }
}
```

#### 8.2.3 银行流水对账（预留接口）

```java
public class BankReconcileService {
  public ReconcileResult importBankStatement(MultipartFile file) {
    // 解析银行流水 Excel/CSV
    // 与 oa_payment_log 对比
    // 输出未匹配项
  }
}
```

### 8.3 验收标准

- [ ] 个税累计预扣法通过国家税务总局公开税率表验证
- [ ] 财务月结流程支持回滚
- [ ] 银行对账预留接口（不强制实现）

---

## 9. 通讯录/日程/消息补齐 🟡 重构

### 9.1 现状问题

- `system/message/` 5 个子目录全空，**完全缺失**
- 日程无周期性、循环提醒
- 通讯录无分组、外部联系人

### 9.2 消息模块从零设计

```sql
-- 消息表（扩展 oa_message）
CREATE TABLE oa_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  msg_type VARCHAR(20) NOT NULL COMMENT 'SYSTEM/APPROVAL/ATTENDANCE/ANNOUNCEMENT/CC',
  title VARCHAR(200) NOT NULL,
  content TEXT,
  sender_id BIGINT COMMENT '0=系统',
  receiver_id BIGINT NOT NULL,
  business_type VARCHAR(50) COMMENT '关联业务类型',
  business_id BIGINT COMMENT '关联业务ID',
  is_read TINYINT DEFAULT 0,
  read_time DATETIME,
  is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_receiver_read (receiver_id, is_read, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 消息偏好设置
CREATE TABLE oa_message_preference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  approval_notify TINYINT DEFAULT 1,
  attendance_notify TINYINT DEFAULT 1,
  announcement_notify TINYINT DEFAULT 1,
  cc_notify TINYINT DEFAULT 1,
  dingtalk_push TINYINT DEFAULT 1,
  email_push TINYINT DEFAULT 0,
  quiet_hours_start TIME,
  quiet_hours_end TIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```java
// 统一消息发送服务
@Service
public class MessageService {
  public void send(MessageDTO message) {
    // 1. 持久化到数据库
    OaMessage msg = save(message);
    // 2. 异步推送（钉钉/邮件/站内）
    asyncPush(msg);
    // 3. WebSocket 实时推送
    webSocketPush(msg);
  }

  @Async
  public void asyncPush(OaMessage msg) {
    // 钉钉
    OaMessagePreference pref = prefMapper.selectById(msg.getReceiverId());
    if (pref.getDingtalkPush() == 1) {
      dingTalkClient.sendWorkMessage(msg.getReceiverId(), msg.getTitle(), msg.getContent());
    }
    // 邮件
    if (pref.getEmailPush() == 1) {
      emailClient.send(msg.getReceiverId(), msg.getTitle(), msg.getContent());
    }
  }
}
```

### 9.3 日程模块扩展

```sql
ALTER TABLE oa_schedule
  ADD COLUMN repeat_type VARCHAR(20) DEFAULT 'NONE'
    COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
  ADD COLUMN repeat_end_date DATE COMMENT '重复结束日期',
  ADD COLUMN reminder_type VARCHAR(20) DEFAULT 'APP'
    COMMENT 'APP/EMAIL/DINGTALK/SMS',
  ADD COLUMN reminder_minutes INT DEFAULT 15 COMMENT '提前提醒分钟',
  ADD COLUMN related_type VARCHAR(50) COMMENT '关联业务类型',
  ADD COLUMN related_id BIGINT COMMENT '关联业务ID';
```

### 9.4 通讯录扩展

- 部门分组：员工/外包/合作伙伴
- 共享名片：员工可把名片共享给外部访客
- 名片导出：PDF/VCF

### 9.5 验收标准

- [ ] 消息中心 8 个端点全部实现
- [ ] 至少 3 个业务（审批/考勤/公告）接入消息推送
- [ ] WebSocket 实时推送（用 Spring WebSocket）
- [ ] 周期性日程支持 DAILY/WEEKLY/MONTHLY

---

## 10. 用印模块去重 🟡 重构

### 10.1 现状

- `solidoa-system/entity/Stamp.java` + `StampController.java`（6 端点）
- `solidoa-workflow/entity/Stamp.java` + `StampController.java`（11 端点）
- 两套物理表 `oa_stamp`

### 10.2 统一方案

**保留 workflow 模块**（用印本质是审批流一环），删除 system 模块重复。

```bash
# 删除文件
solidoa-system/src/main/java/com/solidoa/system/controller/StampController.java
solidoa-system/src/main/java/com/solidoa/system/service/StampService.java
solidoa-system/src/main/java/com/solidoa/system/service/impl/StampServiceImpl.java
solidoa-system/src/main/java/com/solidoa/system/entity/Stamp.java
solidoa-system/src/main/java/com/solidoa/system/form/StampForm.java
solidoa-system/src/main/java/com/solidoa/system/vo/StampVO.java
solidoa-system/src/main/java/com/solidoa/system/mapper/StampMapper.java
solidoa-system/src/main/resources/mapper/StampMapper.xml
```

### 10.3 验证

- 前端 stamp-manage、stamp-record 路由**改用 workflow 服务的 API**
- 确认 `oa_system.oa_stamp` 表**物理删除**（DML 前先备份）

### 10.4 验收标准

- [ ] `system` 模块下无 Stamp 相关文件
- [ ] 前端 stamp-manage 路由指向 workflow 服务
- [ ] 申请/审批/盖章/归还/查询流程跑通

---

## 11. 公告通知模块 🔴 全新设计

### 11.1 数据库设计

```sql
CREATE DATABASE oa_notice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 公告表
CREATE TABLE oa_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(50) NOT NULL COMMENT 'NOTICE/NEWS/POLICY/ACTIVITY',
  summary VARCHAR(500) COMMENT '摘要',
  content LONGTEXT NOT NULL,
  cover_image VARCHAR(500) COMMENT '封面图',
  publisher_id BIGINT NOT NULL,
  publish_time DATETIME,
  expire_time DATETIME COMMENT '过期时间',
  is_top TINYINT DEFAULT 0,
  is_draft TINYINT DEFAULT 0,
  view_count INT DEFAULT 0,
  require_read TINYINT DEFAULT 0 COMMENT '是否要求已读回执',
  target_type VARCHAR(20) DEFAULT 'ALL' COMMENT 'ALL/DEPT/ROLE/USER',
  target_ids VARCHAR(2000) COMMENT 'JSON 数组',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category_top (category, is_top, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 已读回执表
CREATE TABLE oa_notice_read (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  read_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_notice_user (notice_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 公告分类
CREATE TABLE oa_notice_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(50) NOT NULL UNIQUE,
  sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 11.2 核心端点

| 端点                            | 方法   | 说明   |
| ----------------------------- | ---- | ---- |
| `/v1/notices`                 | POST | 发布公告 |
| `/v1/notices`                 | GET  | 分页查询 |
| `/v1/notices/{id}`            | GET  | 详情   |
| `/v1/notices/{id}/read`       | POST | 标记已读 |
| `/v1/notices/{id}/read-stats` | GET  | 已读统计 |
| `/v1/notices/my-published`    | GET  | 我发布的 |
| `/v1/notices/my-unread`       | GET  | 我的未读 |
| `/v1/notices/categories`      | GET  | 分类列表 |

### 11.3 前端页面

- `/notice` — 公告首页（按分类 Tab）
- `/notice/publish` — 发布公告（富文本编辑器）
- `/notice/detail/:id` — 公告详情（含已读回执）
- `/notice/my-published` — 我发布的
- `/notice/my-unread` — 我的未读

### 11.4 验收标准

- [ ] 富文本编辑器支持图片/附件上传
- [ ] 已读回执支持按部门/角色统计
- [ ] 发布后实时推送到消息中心 + 钉钉

---

## 12. 资产管理模块 🔴 全新设计

### 12.1 数据库设计

```sql
CREATE DATABASE oa_asset DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 资产台账
CREATE TABLE oa_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_code VARCHAR(50) NOT NULL UNIQUE COMMENT '资产编号',
  asset_name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL COMMENT '资产分类',
  brand VARCHAR(50),
  model VARCHAR(100),
  purchase_date DATE,
  purchase_price DECIMAL(10,2),
  supplier VARCHAR(200),
  warranty_expire DATE,
  location VARCHAR(200) COMMENT '存放位置',
  custodian_id BIGINT COMMENT '保管人',
  status VARCHAR(20) DEFAULT 'IDLE' COMMENT 'IDLE/IN_USE/REPAIR/SCRAPPED',
  image VARCHAR(500),
  remark TEXT,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 资产分类
CREATE TABLE oa_asset_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(50) NOT NULL UNIQUE,
  sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 资产领用/归还
CREATE TABLE oa_asset_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  trans_type VARCHAR(20) NOT NULL COMMENT 'TAKE/RETURN/TRANSFER/REPAIR/SCRAP',
  user_id BIGINT NOT NULL,
  dept_id BIGINT,
  trans_time DATETIME NOT NULL,
  expected_return_date DATE COMMENT '预计归还日期',
  actual_return_date DATE,
  approval_id BIGINT COMMENT '关联审批ID',
  remark TEXT,
  INDEX idx_asset (asset_id),
  INDEX idx_user (user_id, trans_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 资产盘点
CREATE TABLE oa_asset_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inventory_name VARCHAR(200) NOT NULL,
  start_time DATETIME,
  end_time DATETIME,
  status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/COMPLETED',
  total_count INT DEFAULT 0,
  matched_count INT DEFAULT 0,
  diff_count INT DEFAULT 0,
  creator_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 12.2 端点

| 端点                         | 方法       | 说明      |
| -------------------------- | -------- | ------- |
| `/v1/assets`               | GET/POST | 资产台账    |
| `/v1/assets/{id}/take`     | POST     | 领用（需审批） |
| `/v1/assets/{id}/return`   | POST     | 归还      |
| `/v1/assets/{id}/transfer` | POST     | 调拨      |
| `/v1/assets/{id}/scrap`    | POST     | 报废（需审批） |
| `/v1/assets/inventory`     | POST/GET | 盘点任务    |
| `/v1/assets/stats`         | GET      | 资产统计    |

### 12.3 前端页面

- `/asset/list` — 资产台账（用 `<oa-table>`）
- `/asset/category` — 资产分类管理
- `/asset/my-assets` — 我的资产
- `/asset/inventory` — 盘点任务
- `/asset/stats` — 资产统计图表

### 12.4 验收标准

- [ ] 资产二维码生成（扫码查看详情）
- [ ] 领用/归还/调拨/报废走审批流
- [ ] 盘点支持扫码/手动两种方式
- [ ] 资产统计支持按分类/部门/状态筛选

## 13. 知识文档模块 🔴 全新设计

### 13.1 数据库设计

```sql
CREATE DATABASE oa_knowledge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 文档库
CREATE TABLE oa_doc_library (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  parent_id BIGINT DEFAULT 0,
  permission_type VARCHAR(20) DEFAULT 'PRIVATE' COMMENT 'PRIVATE/DEPT/PUBLIC',
  owner_id BIGINT NOT NULL,
  sort INT DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 文档
CREATE TABLE oa_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  library_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content LONGTEXT,
  file_id BIGINT COMMENT '关联文件服务',
  current_version INT DEFAULT 1,
  author_id BIGINT NOT NULL,
  view_count INT DEFAULT 0,
  star_count INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
  tags VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_library (library_id, status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 文档版本
CREATE TABLE oa_document_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  version INT NOT NULL,
  content LONGTEXT,
  file_id BIGINT,
  change_note VARCHAR(500),
  author_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_doc_version (document_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 文档权限
CREATE TABLE oa_document_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  principal_type VARCHAR(20) NOT NULL COMMENT 'USER/DEPT/ROLE',
  principal_id BIGINT NOT NULL,
  perm_type VARCHAR(20) NOT NULL COMMENT 'READ/EDIT/DELETE/MANAGE',
  UNIQUE KEY uk_doc_principal (document_id, principal_type, principal_id, perm_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 13.2 端点

- `/v1/doc-libraries` — 文档库 CRUD
- `/v1/documents` — 文档 CRUD
- `/v1/documents/{id}/versions` — 版本历史
- `/v1/documents/{id}/rollback/{v}` — 版本回滚
- `/v1/documents/{id}/permissions` — 权限管理
- `/v1/documents/search` — 全文搜索（基于 Elasticsearch，可选）

### 13.3 前端页面

- `/doc` — 我的文档 + 共享给我
- `/doc/library/:id` — 文档库详情
- `/doc/edit/:id` — 文档编辑器（基于 Tiptap 或 wangEditor）
- `/doc/version/:id` — 版本历史对比

### 13.4 验收标准

- [ ] 富文本编辑器支持图片/表格/代码块
- [ ] 版本对比支持 diff 视图
- [ ] 支持文档星标、分享、评论
- [ ] 全文搜索可基于 MySQL FULLTEXT（V2.0）或 Elasticsearch（V2.1）

---

## 14. 钉钉集成重构 🟡 重构

### 14.1 现状问题

- 顶层旧版 `solidoa-dingtalk/`（27 文件）应删除
- `ConcurrentHashMap` 内存缓存 state（生产风险）
- `new Thread()` 异步处理不规范
- 缺钉钉审批回调、通讯录同步

### 14.2 重构方案

#### 14.2.1 State 缓存迁移 Redis

```java
@Service
public class DingtalkStateService {
  @Autowired
  private StringRedisTemplate redis;

  public void saveState(String state, String userId) {
    redis.opsForValue().set("dingtalk:state:" + state, userId, Duration.ofMinutes(5));
  }

  public String getState(String state) {
    return redis.opsForValue().get("dingtalk:state:" + state);
  }
}
```

#### 14.2.2 异步处理改 `@Async`

```java
@Service
public class DingtalkAttendanceSyncService {
  @Async("dingtalkSyncExecutor")
  public void syncAttendance(Long userId) {
    // 替换 new Thread()
  }
}

@Configuration
public class AsyncConfig {
  @Bean("dingtalkSyncExecutor")
  public Executor dingtalkSyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("dingtalk-sync-");
    return executor;
  }
}
```

#### 14.2.3 钉钉审批回调

```java
@RestController
@RequestMapping("/v1/dingtalk/callback")
public class DingtalkCallbackController {

  @PostMapping("/approval")
  public Result handleApprovalCallback(@RequestBody DingtalkApprovalCallback callback) {
    // 1. 验签
    if (!verifySignature(callback)) return Result.fail(403, "签名错误");
    // 2. 解密
    DingtalkApprovalEvent event = decrypt(callback.getEncrypt());
    // 3. 同步到本地审批记录
    approvalService.syncFromDingtalk(event);
    return Result.success();
  }
}
```

#### 14.2.4 通讯录同步

```java
@Service
public class DingtalkContactSyncService {
  @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
  public void syncAll() {
    List<DingtalkUser> users = dingtalkClient.listAllUsers();
    for (DingtalkUser u : users) {
      contactService.upsert(u);
    }
  }
}
```

### 14.3 验收标准

- [ ] 顶层 `solidoa-dingtalk/` 目录删除
- [ ] 钉钉 state 走 Redis
- [ ] 所有异步处理用 `@Async`
- [ ] 钉钉审批回调走工作流引擎

---

## 15. 缓存层与可观测性 🟠 扩展

### 15.1 缓存层

#### 15.1.1 CacheManager 配置

```java
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(1))
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory)
      .cacheDefaults(config)
      .withCacheConfiguration("oa:user", config.entryTtl(Duration.ofHours(4)))
      .withCacheConfiguration("oa:dept", config.entryTtl(Duration.ofHours(8)))
      .withCacheConfiguration("oa:dict", config.entryTtl(Duration.ofHours(24)))
      .withCacheConfiguration("oa:contact", config.entryTtl(Duration.ofMinutes(30)))
      .withCacheConfiguration("oa:schedule", config.entryTtl(Duration.ofMinutes(10)))
      .build();
  }
}
```

#### 15.1.2 使用示例

```java
@Service
public class ContactServiceImpl implements ContactService {
  @Cacheable(value = "oa:contact", key = "#deptId")
  public List<Contact> listByDept(Long deptId) { ... }

  @CacheEvict(value = "oa:contact", key = "#contact.deptId")
  public Contact create(Contact contact) { ... }
}
```

### 15.2 可观测性

#### 15.2.1 TraceId 透传

```java
// 1. Gateway 注入
@Component
public class TraceIdFilter implements GlobalFilter {
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
    }
    MDC.put("traceId", traceId);
    ServerHttpRequest request = exchange.getRequest().mutate()
      .header("X-Trace-Id", traceId).build();
    return chain.filter(exchange.mutate().request(request).build())
      .then(Mono.fromRunnable(() -> MDC.remove("traceId")));
  }
}

// 2. 下游服务拦截器
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, ...) {
    String traceId = request.getHeader("X-Trace-Id");
    if (traceId != null) MDC.put("traceId", traceId);
    return true;
  }
}

// 3. Feign 拦截器
@Component
public class TraceIdFeignInterceptor implements RequestInterceptor {
  @Override
  public void apply(RequestTemplate template) {
    String traceId = MDC.get("traceId");
    if (traceId != null) template.header("X-Trace-Id", traceId);
  }
}

// 4. logback pattern
<pattern>%d [%thread] [%X{traceId}] [%level] %logger{50} - %msg%n</pattern>
```

#### 15.2.2 Prometheus + Grafana

```yaml
# 每个服务 application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

```yaml
# docker-compose.yml 新增
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports: ["9090:9090"]

grafana:
  image: grafana/grafana:latest
  ports: ["3001:3000"]
```

### 15.3 验收标准

- [ ] 5 个核心接口（Dashboard/Contact/Schedule/User/Dict）走 Redis 缓存
- [ ] 全链路 traceId 可在日志中检索
- [ ] Prometheus + Grafana 监控部署

---

## 16. 数据库规范升级 🟠 扩展

### 16.1 字符集统一（已完成 2026-06-02）

- ✅ 5 个库全部 `utf8mb4_unicode_ci`
- ✅ 64 张表全部 `utf8mb4_unicode_ci`
- ✅ init 脚本 `00_init_databases.sql` 已显式声明 COLLATE

### 16.2 表设计规范（V2.0 新增）

```sql
-- 通用规范
CREATE TABLE xxx (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 业务字段
  -- ...
  -- 审计字段（必含）
  create_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 软删除
  deleted TINYINT DEFAULT 0 COMMENT '0未删 1已删',
  -- 索引（按查询场景）
  INDEX idx_xxx (xxx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表注释';
```

### 16.3 Flyway 引入（可选 V2.1）

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
```

```
db/migration/
├── V1.0.0__init_system.sql
├── V1.0.0__init_workflow.sql
├── V1.1.0__add_charset_fix.sql
├── V2.0.0__add_notice_module.sql
├── V2.0.0__add_asset_module.sql
└── V2.0.1__add_permission_upgrade.sql
```

### 16.4 验收标准

- [ ] 新建表 100% 遵循规范
- [ ] 所有表带 COMMENT
- [ ] 所有 datetime 字段带 DEFAULT CURRENT_TIMESTAMP

---

## 17. 部署与运维规范 🟢 保持 + 🟠 扩展

### 17.1 Docker Compose 升级

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --init-connect=SET NAMES utf8mb4
      - --default-authentication-plugin=mysql_native_password
    # ... 已配置良好

  # 新增服务
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    ports: ["3001:3000"]
    depends_on: [prometheus]

  # V2.0 新增微服务
  notice-service:
    build: ...
    environment:
      DB_HOST: mysql
      DB_NAME: oa_notice
    ports: ["8083:8083"]

  asset-service:
    # ...
    ports: ["8088:8088"]
```

### 17.2 CI/CD（GitHub Actions）

```yaml
# .github/workflows/build.yml
name: Build
on: [push, pull_request]
jobs:
  build-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17' }
      - run: mvn clean package -DskipTests
      - uses: actions/upload-artifact@v4
        with: { name: jars, path: '**/target/*.jar' }

  build-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: cd solidoa-web && npm ci
      - run: npm run lint
      - run: npm run type-check
      - run: npm run test
      - run: npm run build
```

### 17.3 验收标准

- [ ] Docker Compose 一键拉起全栈
- [ ] GitHub Actions 跑通 lint + build + test
- [ ] 镜像推送到阿里云/腾讯云镜像仓库

---

## 18. 实施路线图与验收标准

### 18.1 路线图（6 个月 8 个迭代）

| 迭代            | 时间        | 目标              | 关键产出                                      |
| ------------- | --------- | --------------- | ----------------------------------------- |
| **Sprint 1**  | 第 1-2 周   | 基础设施            | 字符集统一完成、TraceId 透传、Message 模块补齐           |
| **Sprint 2**  | 第 3-4 周   | 工程化 + 设计系统      | TS/ESLint/Vitest 落地、design token、核心组件 8 个 |
| **Sprint 3**  | 第 5-6 周   | 审批重构 + Stamp 去重 | Spring State Machine、Stamp 合并、流程图组件       |
| **Sprint 4**  | 第 7-8 周   | 缓存 + 可观测性       | Redis 缓存、Prometheus/Grafana、日志脱敏          |
| **Sprint 5**  | 第 9-10 周  | P0 业务模块         | 公告通知、我的申请                                 |
| **Sprint 6**  | 第 11-12 周 | P1 业务模块         | 资产管理、钉钉集成重构                              |
| **Sprint 7**  | 第 13-14 周 | P2 业务模块         | 知识文档、投票、问卷                                |
| **Sprint 8**  | 第 15-16 周 | HR 完整闭环         | 招聘、培训、绩效、个税                               |
| **Sprint 9**  | 第 17-18 周 | 分析与集成           | BI 看板、SSO、开放 API                          |
| **Sprint 10** | 第 19-20 周 | 移动端 + 验收        | solidoa-app 完善、UAT、性能测试                   |
| **Sprint 11** | 第 21-22 周 | 性能压测 + 上线       | 1000 并发压测、灰度发布                            |
| **Sprint 12** | 第 23-24 周 | 培训 + 试运行        | 用户培训、问题修复、试运行                             |

### 18.2 关键里程碑

- **M1 (Sprint 2 末)**：设计系统落地，第一个页面重构
- **M2 (Sprint 4 末)**：工程化完整，所有新代码走 TypeScript
- **M3 (Sprint 6 末)**：核心业务闭环，泛微 P0 功能 100% 覆盖
- **M4 (Sprint 8 末)**：泛微 P1 功能 100% 覆盖
- **M5 (Sprint 12 末)**：V2.0 正式发布

### 18.3 验收标准（V2.0 发布条件）

#### 功能验收

- [ ] 9 个微服务全部就绪（5 个原有 + 4 个新增）
- [ ] 至少 60 个页面（V1.x 34 + 新增 26）
- [ ] 13 个核心组件 + 5 个业务组件
- [ ] 4 个缺失大模块（公告/资产/知识/招聘培训）全部上线

#### 质量验收

- [ ] TypeScript 100% 覆盖
- [ ] ESLint 0 errors
- [ ] Vitest 单元测试覆盖率 ≥ 60%
- [ ] 关键业务 E2E 测试通过

#### 性能验收

- [ ] 1000 并发用户 TPS ≥ 200
- [ ] 首页 P95 < 1.5s
- [ ] API P95 < 500ms

#### 稳定性验收

- [ ] 7×24 试运行 0 重大故障
- [ ] 监控告警完整（Prometheus + Grafana）
- [ ] 文档完整（API 文档 + 用户手册 + 运维手册）

### 18.4 风险与应对

| 风险                        | 等级  | 应对                        |
| ------------------------- | --- | ------------------------- |
| Spring State Machine 学习曲线 | 中   | 先用 V1.x 状态机封装做 POC，再升级    |
| 设计系统迁移工作量                 | 高   | 分页面分批迁移，每 Sprint 重构 5-8 页 |
| Seata 引入可能影响性能            | 中   | 仅在核心链路（请假扣减余额）使用          |
| 钉钉回调稳定性                   | 中   | 走 MQ 异步 + 重试机制            |
| 业务数据迁移（合并表）               | 高   | Stamp 合并前全量备份，预生产环境演练     |

---

## 附录

### A. 关键文件清单

**需要删除的旧文件**（重构第一步）

- `solidoa/solidoa-common/` （4 个文件）
- `solidoa/solidoa-dingtalk/` （27 个文件）
- `solidoa-parent/solidoa-system/src/main/java/com/solidoa/system/controller/StampController.java`（含配套 6 个文件）
- `solidoa-parent/solidoa-system/src/main/java/com/solidoa/system/message/`（5 个空目录）

**需要新增的核心文件**

- `solidoa-web/src/styles/tokens.scss`
- `solidoa-web/src/styles/mixins.scss`
- `solidoa-web/src/styles/element-overrides.scss`
- `solidoa-web/src/components/OaTable/index.vue`（+ 12 个组件）
- `solidoa-web/src/composables/useTable.js`（+ useForm/useDialog/useApproval）
- `solidoa-web/tsconfig.json`
- `solidoa-web/.eslintrc.cjs`
- `solidoa-parent/solidoa-workflow/statemachine/ApprovalStateMachineConfig.java`
- `solidoa-parent/solidoa-notice/`（新模块）
- `solidoa-parent/solidoa-asset/`（新模块）
- `solidoa-parent/solidoa-knowledge/`（新模块）

### B. 参考资料

- 泛微 e-cology V10 产品手册
- Spring State Machine 官方文档
- Element Plus 主题定制指南
- Pinia 2 状态管理实践
- Vite 5 性能优化指南

---

**文档结束**
