# SolidOA 全维度修复指南

> 生成时间：2026-05-27
> 最后审查：2026-05-27（全维度二次审查更新）
> 基于全系统审查发现的问题，按优先级分类整理

---

## 修复状态说明

- `[ ]` 未修复
- `[x]` 已修复
- `[-]` 不适用（经复核确认无需修改）

---

## 一、安全性修复（P0 - 立即修复）

### 1.1 移除硬编码密码

**问题：** 所有 application.yml 中的数据库密码、JWT Secret、MinIO 密钥等敏感信息作为默认值硬编码在代码中。

**修复方法：** 移除默认值，只保留环境变量引用。Docker/生产环境必须通过环境变量注入。

#### 需要修改的文件：

**solidoa-system/application.yml** (第20-22行)
```yaml
# 修改前
url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3307}/oa_system?...
username: ${DB_USER:root}
password: ${DB_PASSWORD:749958714}
# 修改后
url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
username: ${DB_USER}
password: ${DB_PASSWORD}
```

**solidoa-gateway/application.yml** (第91-97行)
```yaml
# 修改前
host: ${REDIS_HOST:localhost}
port: ${REDIS_PORT:6379}
password: ${REDIS_PASSWORD:}
...
secret: ${JWT_SECRET:solidoa-secret-key-change-in-production}
# 修改后
host: ${REDIS_HOST}
port: ${REDIS_PORT}
password: ${REDIS_PASSWORD}
...
secret: ${JWT_SECRET}
```

**solidoa-common/application.yml** (第7-9、17-21、32行)
```yaml
# 数据库
password: ${DB_PASSWORD}
# Redis
host: ${REDIS_HOST}
password: ${REDIS_PASSWORD}
# RabbitMQ
username: ${RABBITMQ_USER}
password: ${RABBITMQ_PASSWORD}
# JWT
secret: ${JWT_SECRET}
```

**solidoa-workflow/application.yml** (第21-23、30-33行)
```yaml
password: ${DB_PASSWORD}
username: ${RABBITMQ_USER}
password: ${RABBITMQ_PASSWORD}
```

**solidoa-collaboration/application.yml** (第21、28行)
```yaml
password: ${DB_PASSWORD}
secret: ${JWT_SECRET}  # 注意：当前默认值与其他服务不一致(defaultSecretKey123456789012345678901234567890)
```

**solidoa-finance/application.yml** (第21行)
```yaml
password: ${DB_PASSWORD}
```

**solidoa-attendance/application.yml** (第21行)
```yaml
password: ${DB_PASSWORD}
```

**solidoa-file/application.yml** (第21、29-30行)
```yaml
password: ${DB_PASSWORD}
access-key: ${MINIO_ACCESS_KEY}
secret-key: ${MINIO_SECRET_KEY}
```

**solidoa-dingtalk/application.yml** (第10-12、17行)
```yaml
# Nacos 密码（原文档遗漏）
username: ${NACOS_USERNAME}
password: ${NACOS_PASSWORD}
# 数据库
password: ${DB_PASSWORD}
```

### 1.2 统一数据库端口

**问题：** 部分服务使用 3306，部分使用 3307，Docker 环境会连接失败。

| 服务 | 当前端口 |
|------|----------|
| solidoa-common | 3306 |
| solidoa-system | 3307 |
| solidoa-workflow | 3307 |
| solidoa-collaboration | 3306 |
| solidoa-finance | 3307 |
| solidoa-attendance | 3307 |
| solidoa-file | 3306 |
| solidoa-dingtalk | 3306 |

**修复方法：** 统一使用环境变量，不设默认端口差异。

```yaml
# 所有服务统一
url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

### 1.3 更新 .gitignore

**文件：** `.gitignore`

**当前状态：** 缺少敏感配置文件忽略规则。

**添加以下内容：**
```gitignore
# Environment files
.env
*.env
!.env.example

# Sensitive config
**/application-local.yml
**/application-prod.yml

# Docker
docker/.env
```

---

## 二、数据正确性修复（P0）

### 2.1 修复种子数据语义错误

**文件：** `solidoa/sql/init/07_init_data.sql` (第29-33行)

**问题：** leave_type 字典值与标签完全颠倒。

```sql
-- 修改前（错误）
INSERT INTO sys_dict (type, label, value, sort) VALUES
('leave_type', '病假', 'ANNUAL', 1),
('leave_type', '事假', 'SICK', 2),
('leave_type', '调休', 'PERSONAL', 3),
('leave_type', '出差', 'BUSINESS', 4);

-- 修改后（正确）
INSERT INTO sys_dict (type, label, value, sort) VALUES
('leave_type', '年假', 'ANNUAL', 1),
('leave_type', '病假', 'SICK', 2),
('leave_type', '事假', 'PERSONAL', 3),
('leave_type', '调休', 'COMPENSATORY', 4),
('leave_type', '出差', 'BUSINESS', 5);
```

### 2.2 统一 DDL 脚本

**问题：** `08_overtime.sql` 与 `V1.3__attendance_and_salary_tables.sql` 中 `oa_overtime` 和 `oa_overtime_break` 表结构冲突。

**冲突分析：**

| 字段 | 08_overtime.sql | V1.3 | Java 实体类 |
|------|----------------|------|-------------|
| 时长 | `hours` | `total_hours` | `hours` ✅ |
| 补偿方式 | `compensation_type` | 无 | `compensationType` ✅ |
| 审批人 | `current_approver_id` | `approver_id` | `currentApproverId` ✅ |
| 审批时长 | `approved_hours` | 无 | `approvedHours` ✅ |
| 加班日期 | 无 | `overtime_date` | 无 |
| 附件 | 无 | `attachments` | 无 |

**结论：** Java 实体 `Overtime.java`、`OvertimeBreak.java` 及 `OvertimeServiceImpl.java` 全部匹配 `08_overtime.sql` 的字段定义。V1.3 是后续扩写的设计，但代码未同步更新。

**修复方法：** 删除 `V1.3__attendance_and_salary_tables.sql` 中第115-173行的 `oa_overtime` 和 `oa_overtime_break` 建表语句（保留其他17张表）。`08_overtime.sql` 作为正确版本保留。

---

## 三、后端代码修复（P1）

### 3.1 修复 WorkflowClient 返回类型错误

**文件：** `solidoa-common/src/main/java/com/solidoa/common/client/WorkflowClient.java` (第19-20行)

```java
// 修改前
@GetMapping("/expense/{id}")
Result<LeaveDTO> getExpenseById(@PathVariable("id") Long id);

// 修改后
@GetMapping("/expense/{id}")
Result<ExpenseDTO> getExpenseById(@PathVariable("id") Long id);
```

### 3.2 修复 RolePermission 缺少注解

**文件：** `solidoa-system/src/main/java/com/solidoa/system/entity/RolePermission.java`

```java
// 修改前
@Data
public class RolePermission {

// 修改后
@Data
@TableName("sys_role_permission")
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    // ... 其他字段
}
```

### 3.3 建立统一错误码枚举

**当前状态：** ErrorCode 枚举不存在。项目仅有通用的 `Result.fail(code, message)` 模式。

**步骤一：新建文件** `solidoa-common/src/main/java/com/solidoa/common/constant/ErrorCode.java`

```java
package com.solidoa.common.constant;

public enum ErrorCode {
    // 通用错误 1000-1999
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户模块 1000-1099
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已禁用"),
    USER_EXISTS(1004, "用户已存在"),

    // 审批模块 2000-2099
    APPROVAL_NOT_FOUND(2001, "审批记录不存在"),
    APPROVAL_ALREADY_PROCESSED(2002, "该审批已处理"),
    APPROVAL_NO_PERMISSION(2003, "无审批权限"),

    // 考勤模块 3000-3099
    ATTENDANCE_ALREADY_CLOCKED(3001, "今日已打卡"),
    ATTENDANCE_NOT_WORKDAY(3002, "非工作日"),

    // 财务模块 4000-4099
    BUDGET_EXCEEDED(4001, "预算超支"),
    EXPENSE_NOT_FOUND(4002, "报销单不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
```

**步骤二：扩展 BusinessException 支持 ErrorCode 枚举**

**文件：** `solidoa-common/src/main/java/com/solidoa/common/exception/BusinessException.java`

```java
// 添加新构造函数
public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.code = errorCode.getCode();
}

public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.code = errorCode.getCode();
}
```

### 3.4 统一异常处理 - 使用 BusinessException

**需要修改的文件：** `solidoa-workflow/.../StampServiceImpl.java` 等 7 个文件（共 32 处）

**当前状态：** 存在大量 `throw new RuntimeException(...)`。

| 文件 | 出现次数 |
|------|----------|
| StampServiceImpl.java | 11 |
| PurchaseServiceImpl.java | 11 |
| ApprovalFlowServiceImpl.java | 3 |
| ApprovalCcServiceImpl.java | 2 |
| ApprovalNodeServiceImpl.java | 2 |
| WorkflowMessageConsumer.java | 2 |
| ApprovalRecordController.java | 1 |

将所有 `throw new RuntimeException(...)` 替换为 `throw new BusinessException(ErrorCode.XXX)`。

```java
// 修改前
throw new RuntimeException("用印申请不存在");

// 修改后（扩展 BusinessException 后）
throw new BusinessException(ErrorCode.NOT_FOUND, "用印申请不存在");

// 或（未扩展前，使用现有构造函数）
throw new BusinessException(404, "用印申请不存在");
```

### 3.5 修复编号生成器

**复核结论：** [-] 不适用。经实际检查，编号生成器使用 Hutool 的 `IdUtil.fastSimpleUUID()` 和 `IdUtil.getSnowflakeNextId()`，`StampServiceImpl` 中的编号生成采用数据库查询最大序号方式，非内存 AtomicLong 计数器。无需修改。

> ~~原文档描述使用 AtomicLong 进程内计数器~~ → 实际为数据库序号查询 + Hutool 雪花ID，已具备多实例安全性。

### 3.6 修复 PageUtil.validatePageParams

**文件：** `solidoa-common/.../PageUtil.java` (第34-44行)

**问题：** 方法参数为 `Integer`（不可变对象，值传递），仅修改了局部变量，调用方原始值不变，校验完全无效。

```java
// 当前签名
public static void validatePageParams(Integer pageNum, Integer pageSize)

// 修改方案：改为接受 PageDTO 对象
public static void validatePageParams(PageDTO pageDTO) {
    if (pageDTO.getPageNum() == null || pageDTO.getPageNum() < 1) {
        pageDTO.setPageNum(1);
    }
    if (pageDTO.getPageSize() == null || pageDTO.getPageSize() < 1) {
        pageDTO.setPageSize(20);
    }
    if (pageDTO.getPageSize() > 100) {
        pageDTO.setPageSize(100);
    }
}
```

> 注：该类已提供 `safePageNum` 和 `safePageSize` 静态方法作为正确替代，但 `validatePageParams` 本身是死代码陷阱，应修复或移除。

---

## 四、前端修复（P1）

### 4.1 修复路由权限守卫

**文件：** `solidoa-web/src/router/index.js` (第52-58行)

**当前状态：** 仅检查登录状态，未检查路由 `meta.roles` 权限。部分路由已声明 `roles: ['ADMIN']` 但守卫未做比对。

```javascript
// 修改后
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 未登录跳转登录页
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
    return
  }

  // 检查路由权限
  if (to.meta.roles && to.meta.roles.length > 0) {
    const userRoles = userStore.roles || []
    const hasPermission = to.meta.roles.some(role => userRoles.includes(role))
    if (!hasPermission) {
      next('/403')
      return
    }
  }

  next()
})
```

### 4.2 修复 getUserInfo 对接真实 API

**文件：** `solidoa-web/src/stores/user.js` (第43-49行)

**当前状态：** 直接返回硬编码对象 `{ id: 1, username: 'admin', realName: '系统管理员' }`，未调用后端 API。

**前置条件（三步缺一不可）：**

**步骤一：后端新增接口** `UserController.java`

```java
@GetMapping("/users/current")
public Result<Map<String, Object>> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(userService.getCurrentUser(userId));
}
```

`UserService` 新增 `getCurrentUser` 方法，查询用户信息 + 角色列表，返回结构：
```json
{
  "id": 1,
  "username": "admin",
  "realName": "系统管理员",
  "roles": ["SYSTEM_ADMIN"]
}
```

**步骤二：前端** `api/system.js` 新增方法：
```javascript
getCurrentUser: () => request.get('/v1/system/users/current'),
```

**步骤三：** `stores/user.js` 顶部导入：
```javascript
import { systemApi } from '@/api/system'
```

**修复代码：**

```javascript
// 修改前（非 async，硬编码 mock）
getUserInfo() {
  this.userInfo = { id: 1, username: 'admin', realName: '系统管理员' }
}

// 修改后（需加 async 关键字）
async getUserInfo() {
  try {
    const res = await systemApi.getCurrentUser()
    this.userInfo = res.data
    this.roles = res.data.roles || []
    return this.userInfo
  } catch (error) {
    this.token = ''
    this.userInfo = null
    localStorage.removeItem('token')
    throw error
  }
}
```

**注意：** 原方法不是 `async`，`login` 方法第32行 `this.getUserInfo()` 需改为 `await this.getUserInfo()`。

### 4.3 修复 401 自动跳转

**文件：** `solidoa-web/src/utils/request.js` (响应拦截器)

**当前状态：** 401 处理仅显示错误消息，未清除 token、未跳转登录页。

```javascript
// 在 401 处理处添加
if (response.status === 401 || (response.data && response.data.code === 401)) {
  ElMessage.error('登录已过期，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  window.location.href = '/login'
  return Promise.reject(error)
}
```

### 4.4 修复通讯录变量错误（BUG）

**文件：** `solidoa-web/src/views/collab/contact/index.vue` (第64行)

```html
<!-- 修改前 -->
<button class="action-btn call-btn" @click="handleCall(row)">

<!-- 修改后 -->
<button class="action-btn call-btn" @click="handleCall(item)">
```

### 4.5 修复请假表单验证（BUG）

**文件：** `solidoa-web/src/views/workflow/leave/index.vue`

**问题：** `formRef` 在第155行声明，但模板使用原生 HTML 元素而非 `<el-form>`，运行时 `formRef.value` 为 `undefined`，调用 `.validate()` 会抛出 TypeError。

**修复方案：** 改为手动验证（推荐，保持现有自定义表单风格）

```javascript
const handleSubmit = async () => {
  if (!form.leaveType) {
    ElMessage.warning('请选择请假类型')
    return
  }
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择请假时间')
    return
  }
  if (!form.reason) {
    ElMessage.warning('请输入请假事由')
    return
  }
  // ... 提交逻辑
}
```

### 4.6 提取公共组件

**当前状态：** BaseDialog、StatusBadge、TabBar 组件均不存在。项目中弹窗、状态徽章、Tab 均为各页面内联实现。

**新建目录：** `solidoa-web/src/components/`

#### 4.6.1 BaseDialog.vue
```vue
<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog" :style="{ width }">
        <div class="dialog-header">
          <h3>{{ title }}</h3>
          <button class="close-btn" @click="$emit('close')">&times;</button>
        </div>
        <div class="dialog-body">
          <slot></slot>
        </div>
        <div class="dialog-footer" v-if="$slots.footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  width: { type: String, default: '500px' }
})
defineEmits(['close'])
</script>
```

#### 4.6.2 StatusBadge.vue
```vue
<template>
  <span class="status-badge" :class="statusClass">{{ statusText }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const statusMap = {
  PENDING: { text: '审批中', class: 'pending' },
  APPROVED: { text: '已通过', class: 'approved' },
  REJECTED: { text: '已拒绝', class: 'rejected' },
  CANCELLED: { text: '已撤回', class: 'cancelled' }
}

const statusClass = computed(() => statusMap[props.status]?.class || '')
const statusText = computed(() => statusMap[props.status]?.text || props.status)
</script>

<style scoped>
.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}
.pending { background: #FFF7E6; color: #D48806; }
.approved { background: #F6FFED; color: #52C41A; }
.rejected { background: #FFF2F0; color: #FF4D4F; }
.cancelled { background: #F5F5F5; color: #999; }
</style>
```

#### 4.6.3 TabBar.vue
```vue
<template>
  <div class="tab-bar">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      class="tab-item"
      :class="{ active: modelValue === tab.value }"
      @click="$emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  tabs: { type: Array, required: true },
  modelValue: { type: String, required: true }
})
defineEmits(['update:modelValue'])
</script>
```

### 4.7 添加 Loading 状态

**需要修改的页面：** leave、expense、user、contact 等 20+ 页面

```vue
<template>
  <div class="table-wrapper" v-loading="loading">
    <!-- 表格内容 -->
  </div>
</template>

<script setup>
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    // ... 加载数据
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}
</script>
```

---

## 五、UI 设计修复（P2）

### 5.1 统一主题色

**文件：** `solidoa-web/src/styles/variables.scss`

```scss
// 修改前
--primary-color: #409eff;

// 修改后（与 DESIGN.md 一致）
--primary-color: #60A5FA;
```

**文件：** `solidoa-web/src/views/login/index.vue` (第149行)

```scss
// 修改前
$primary: #3B82F6;

// 修改后
$primary: #60A5FA;
```

### 5.2 修复颜色对比度

**文件：** 多个页面中的 `$text-secondary`

```scss
// 修改前（对比度 2.76:1，不达标）
$text-secondary: #9CA3AF;

// 修改后（对比度 4.54:1，达到 WCAG AA）
$text-secondary: #6B7280;
```

### 5.3 清理残留的赛博朋克样式

**问题：** 比初次发现范围更广，共 12 个文件存在残留。

**需要清理的文件及残留内容：**

| 文件 | 残留类名 | 残留 neon 配色 |
|------|----------|----------------|
| `collab/contact/index.vue` | bg-grid, scanline, glitch, cyber-card, cyber-input, cyber-btn, cyber-pagination | `#0af`, `#c084fc`, `#00ff9d`, `#ffb700`, `#ff3366` |
| `collab/message/index.vue` | bg-grid, scanline | `#0af`, `#c084fc`, `#00ff9d` |
| `collab/schedule/index.vue` | bg-grid, scanline, cyber-card, cyber-btn, cyber-calendar, cyber-dialog, cyber-form, cyber-input | `#c084fc`, `#00ff9d`, `#f97316` |
| `system/user/index.vue` | bg-grid, scanline | - |
| `system/dept/index.vue` | bg-grid, scanline | - |
| `system/role/index.vue` | bg-grid, scanline | - |
| `attendance/leave-balance/index.vue` | bg-grid | - |
| `system/attendance/leave-types/index.vue` | bg-grid | - |
| `system/attendance/rules/index.vue` | bg-grid | - |
| `system/attendance/shifts/index.vue` | bg-grid | - |
| `system/attendance/holidays/index.vue` | bg-grid | - |
| `system/attendance/groups/index.vue` | bg-grid | - |

**修复方法：**
1. 移除 HTML 中的 `bg-grid`、`scanline`、`glitch` 类引用及其对应的 CSS 定义
2. `collab/contact/index.vue` 和 `collab/schedule/index.vue` 中的 `cyber-card`、`cyber-input`、`cyber-btn`、`cyber-pagination` 等类名需重命名为标准类名（如 `.card`、`.search-input`、`.action-btn`），并同步修改 CSS 定义
3. 将 neon 配色替换为设计系统中的马卡龙色：
   - `#0af` → `#60A5FA`（主色）
   - `#c084fc` → `#A78BFA`（紫色）
   - `#00ff9d` → `#34D399`（成功色）
   - `#ffb700` → `#FBBF24`（警告色）
   - `#ff3366` → `#FCA5A5`（危险色）
   - `#f97316` → `#FBBF24`（警告色）
4. 清理后验证页面样式无回退

### 5.4 统一按钮圆角

**与 DESIGN.md 保持一致：**

```scss
// DESIGN.md 定义: 12px
.btn {
  border-radius: 12px;
  padding: 10px 20px;
}
```

---

## 六、依赖与配置修复（P2）

### 6.1 修复 vite.config.js

**文件：** `solidoa-web/vite.config.js`

**问题：** `productionSourceMap` 是 Vue CLI 的配置项，在 Vite 中无效。且当前放在 config 顶层而非 `build` 内部。

```javascript
// 修改前（第37行，config 顶层，无效）
productionSourceMap: false

// 修改后（移入 build 对象内，Vite 正确属性名）
build: {
  rollupOptions: { ... },
  chunkSizeWarningLimit: 500,
  sourcemap: false   // ← 替换 productionSourceMap
}
```

### 6.2 统一 Lombok scope

**问题：** 各模块 Lombok scope 不统一，存在三种写法。

| 当前状态 | 模块 |
|----------|------|
| `provided` | solidoa-workflow、solidoa-attendance |
| `optional` | solidoa-common、solidoa-system、solidoa-collaboration、solidoa-file、solidoa-finance |
| 无 scope | solidoa-gateway |
| 无 lombok 依赖 | solidoa-dingtalk |
| 仅版本声明，无 scope | 根 pom.xml dependencyManagement |

**所有模块 pom.xml 统一为：**

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

**根 pom.xml dependencyManagement 中统一声明版本和 scope：**

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>
```

### 6.3 统一 JWT 版本管理

**当前状态：** 版本已统一为 0.12.5，但各模块各自硬编码版本号。

**文件：** 根 `pom.xml` 的 `dependencyManagement`

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
</dependency>
```

**移除各子模块中的版本号声明。**

### 6.4 删除重复的 pom.xml

**删除以下文件：**
- `solidoa/solidoa-common/pom.xml`（保留 `solidoa-parent/solidoa-common/pom.xml`）
- `solidoa/solidoa-dingtalk/pom.xml`（保留 `solidoa-parent/solidoa-dingtalk/pom.xml`）

### 6.5 修复 solidoa-common/pom.xml 中 JUnit 依赖重复

**文件：** `solidoa-parent/solidoa-common/pom.xml`

**问题：** 第93-98行和第105-115行完全重复声明了 `junit-jupiter-api` 和 `junit-jupiter-engine`。

**修复：** 删除第105-115行的重复声明。

---

## 七、API 设计修复（P2）

### 7.1 补充 Swagger 注解

**示例：UserController.java**

```java
@Tag(name = "用户管理", description = "用户CRUD操作")
@RestController
@RequestMapping("/api/v1/system/users")
public class UserController {

    @Operation(summary = "获取用户列表", description = "分页查询用户列表")
    @GetMapping
    public Result<PageVO<UserVO>> list(
            @ParameterObject PageDTO pageDTO,
            @Parameter(description = "用户名") @RequestParam(required = false) String username) {
        // ...
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        // ...
    }
}
```

### 7.2 统一用户 ID 获取方式

**新建参数解析器：**

```java
@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader("X-User-Id");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.parseLong(userId);
    }
}
```

**使用方式：**
```java
@GetMapping("/list")
public Result<PageVO<LeaveVO>> list(@CurrentUserId Long userId, PageDTO pageDTO) {
    // ...
}
```

### 7.3 修复 PageUtil

见 [3.6 修复 PageUtil.validatePageParams](#36-修复-pageutilvalidatepageparams)

---

## 八、修复顺序建议

### 第一天（安全 + 数据正确性）
- [ ] 移除所有硬编码密码（1.1）
- [ ] 统一数据库端口（1.2）
- [ ] 更新 .gitignore（1.3）
- [ ] 修复种子数据语义错误（2.1）
- [ ] 统一 DDL 脚本（2.2）

### 第二天（后端核心修复）
- [ ] 建立 ErrorCode 枚举（3.3）
- [ ] 修复 WorkflowClient 返回类型（3.1）
- [ ] 修复 RolePermission 注解（3.2）
- [ ] 统一异常处理 RuntimeException → BusinessException（3.4）
- [ ] 修复 PageUtil（3.6）

### 第三天（前端核心修复 + 后端配合）
- [ ] 后端新增 `GET /api/v1/system/users/current` 接口（4.2 前置）
- [ ] 前端 `api/system.js` 新增 `getCurrentUser`（4.2 前置）
- [ ] 修复路由权限守卫（4.1）
- [ ] 修复 getUserInfo 对接真实 API（4.2）
- [ ] 修复 401 自动跳转（4.3）
- [ ] 修复通讯录变量错误 BUG（4.4）
- [ ] 修复请假表单验证 BUG（4.5）

### 第四天（前端组件提取）
- [ ] 创建 BaseDialog 组件（4.6.1）
- [ ] 创建 StatusBadge 组件（4.6.2）
- [ ] 创建 TabBar 组件（4.6.3）
- [ ] 替换各页面中的重复代码
- [ ] 添加 Loading 状态（4.7）

### 第五天（UI + 配置修复）
- [ ] 统一主题色（5.1）
- [ ] 修复颜色对比度（5.2）
- [ ] 清理赛博朋克类名和配色（5.3）- 12个文件
- [ ] 修复 vite.config.js（6.1）
- [ ] 统一 Lombok scope（6.2）

### 第六天（API + 依赖修复）
- [ ] 补充 Swagger 注解（7.1）
- [ ] 统一用户 ID 获取方式（7.2）
- [ ] 统一 JWT 版本管理（6.3）
- [ ] 删除重复的 pom.xml（6.4）

### 第七天（测试验证）
- [ ] 运行后端服务，验证登录流程
- [ ] 运行前端，验证权限控制
- [ ] 测试各模块核心功能
- [ ] 检查控制台无报错

---

## 九、验证清单

### 安全性验证
- [ ] 应用启动时读取环境变量，无硬编码密码
- [ ] 未设置环境变量时启动失败并给出明确提示
- [ ] .gitignore 已忽略敏感配置文件

### 功能验证
- [ ] 登录正常，token 存储正确
- [ ] 路由守卫生效，无权限用户无法访问受限页面
- [ ] getUserInfo 返回真实用户信息（非硬编码）
- [ ] 401 响应自动清除 token 并跳转登录页
- [ ] 请假表单验证正常（不报 TypeError）
- [ ] 通讯录拨号按钮功能正常（不报 ReferenceError）

### 代码质量验证
- [ ] 无 RuntimeException 直接抛出
- [ ] ErrorCode 枚举被正确使用
- [ ] PageUtil.validatePageParams 有效修改对象属性
- [ ] 前端无 console.error 静默吞没
- [ ] 无赛博朋克残留类名和配色

---

## 十、注意事项

1. **备份数据库** - 修改 DDL 脚本前，先备份现有数据
2. **环境变量配置** - 确保部署环境正确配置所有必需的环境变量
3. **逐步修复** - 每完成一个模块，立即测试验证
4. **代码审查** - 修复完成后进行代码审查，确保无遗漏
5. **文档更新** - 修复完成后更新 README 和部署文档

---

## 附录：复核修正说明

以下为二次审查中对初次文档的修正：

| 原文档描述 | 修正后 | 原因 |
|-----------|--------|------|
| 3.5 编号生成器使用 AtomicLong | [-] 不适用 | 实际使用 Hutool IdUtil + 数据库序号查询 |
| 5.3 赛博朋克残留仅3个文件 | 实际12个文件 | 初次审查遗漏，含 system/* 和 attendance/* 模块 |
| 5.3 仅清理类名 | 需同时清理 neon 配色 | #0af、#00ff9d 等 neon 色与设计系统不符 |
| 6.2 Lombok scope 仅部分不一致 | 存在 provided/optional/无声明 三种写法 | 需统一到根 pom dependencyManagement |
| 新增 3.6 PageUtil 问题 | 方法参数为 Integer，值传递导致无效 | 初次文档签名描述为 PageDTO，与实际不符 |
| 新增 1.1 dingtalk Nacos 密码 | dingtalk 服务也有 Nacos 密码硬编码 | 初次文档遗漏 |
| 3.4 BusinessException(ErrorCode.XXX) | 需先扩展 BusinessException 类 | 原类只支持 (String) 和 (int, String) 构造函数 |
| 4.2 getUserInfo 直接 await | 需加 async 关键字 | 原方法非 async，调用处也需改为 await |
| 5.3 contact 残留类名不完整 | 补充 cyber-card/input/btn/pagination | 初次遗漏了 contact 中的 cyber-* 系列类名 |
| 6.2 Lombok scope 表格 | 修正：dingtalk 无 lombok 依赖，外层 common 为 optional | 原表中 dingtalk 和外层 common 的 scope 描述有误 |
| 4.2 systemApi.getUserInfo() | 需三步前置：后端新增接口 + 前端 API 方法 + 导入 systemApi | 原代码直接调用不存在的方法，且 systemApi 未导入 |
| 新增 6.5 JUnit 依赖重复 | solidoa-common/pom.xml 第93行和第105行完全重复 | 初次文档遗漏 |
| 2.2 DDL 冲突"以 V1.3 为准" | 应保留 08_overtime.sql，删除 V1.3 中重复表 | Java 实体类字段全部匹配 08_overtime.sql |
| 4.2 后端 /current 接口 | 需同步新增 UserController.getCurrentUser | 后端当前无此接口，是新增依赖非不确定性 |
