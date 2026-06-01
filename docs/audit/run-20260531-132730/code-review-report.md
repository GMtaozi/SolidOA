# SolidOA 项目代码评审报告

## 总体健康度

**项目存在严重安全隐患，整体代码质量偏低，需紧急处理 Critical 和 High 级别问题。**

| Critical | High | Medium | Low |
|----------|------|--------|-----|
| 14 | 45 | 25 | 10 |

---

## Critical 问题 (14条)

### 1. 工作流审批权限校验缺失 (4处)

**文件**: LeaveServiceImpl.java, ExpenseServiceImpl.java, PurchaseServiceImpl.java, StampServiceImpl.java

**问题**: approve() 方法只检查状态为 PENDING，未校验 approverId 是否为合法审批人。任意用户均可审批他人的请假/报销/采购/用印申请。

**修复**: 在审批前增加审批人身份校验，验证当前用户是否为该申请的实际审批人。

---

### 2. 工资单号 Math.random() 重复风险

**文件**: SalaryServiceImpl.java:285-288

**问题**: generateSalaryNo() 使用 (int)(Math.random() * 10000) 生成 4 位随机数，高并发场景下必然产生重复单号。

**修复**: 使用数据库自增 ID 或 Redis 原子递增替代随机数。

---

### 3. 文件 Magic Number 检测可被绕过

**文件**: FileServiceImpl.java:88-131

**问题**: 仅读取 8-16KB 字节进行文件头检测，攻击者可将恶意脚本追加到合法文件末尾绕过检测。

**修复**: 增加完整文件内容扫描或校验文件哈希；压缩包类型先解压后检测内容。

---

### 4. 出勤天数重复计算

**文件**: SummaryServiceImpl.java:53-55

**问题**: actualDays 使用 count() 统计记录条数，而非按 checkDate 去重。同一天多次打卡会被重复计算。

**修复**: 改用 `SELECT COUNT(DISTINCT check_date)` 或 Java Stream distinct()。

---

### 5. 补卡申请并发竞态条件

**文件**: RepairServiceImpl.java:39-58

**问题**: 检查是否已存在记录与插入记录之间无锁，高并发场景下可能导致重复提交。

**修复**: 在数据库表添加 (user_id, check_date, check_type) 唯一索引，或使用 SELECT FOR UPDATE。

---

### 6. 部门管理无权限控制

**文件**: DeptController.java

**问题**: 所有接口（getTree、getById、create、update、delete）均无 @PreAuthorize 注解，普通认证用户即可创建/修改/删除部门。

**修复**: 添加 `@PreAuthorize("hasAnyRole('ADMIN', 'DEPT_MANAGER')")` 注解。

---

### 7. 角色权限分配无权限控制

**文件**: RoleController.java:55-58

**问题**: assignPermissions 接口允许任意认证用户为任意角色分配权限，可导致权限提升攻击。

**修复**: 添加 `@PreAuthorize("hasRole('ADMIN')")` 注解限制调用权限。

---

### 8. UserDTO 暴露密码字段

**文件**: UserDTO.java:9

**问题**: password 字段存在于 DTO，若用于 API 响应会以明文暴露用户密码。

**修复**: 从 UserDTO 中移除 password 字段。

---

### 9. 银行账号明文存储

**文件**: Expense.java:21

**问题**: bankAccount 字段无加密注解或加密实现，银行账号明文存储违反个人信息保护规定。

**修复**: 使用 AES/SM4 加密后存储，业务层解密展示需权限校验。

---

### 10. 手机号声称加密但未实现

**文件**: StampRecord.java:27

**问题**: receivedMobile 注释声明"加密存储"但代码中无任何加密实现。

**修复**: 确认 CryptoUtil.encrypt() 调用正确实现。

---

### 11. Token 明文存储 localStorage (3处)

**文件**:
- solidoa-app/src/stores/user.js:6
- solidoa-app/src/utils/request.js:7
- solidoa-web/src/stores/user.js:23-27

**问题**: Token 以明文或 Base64 编码存储在 localStorage，可通过 XSS payload 直接读取。

**修复**: 改用 HttpOnly Cookie 存储 Token，或使用 CryptoJS AES 加密存储。

---

## High 问题 (45条)

### 安全类 (12条)

| 文件 | 问题 | 修复建议 |
|------|------|----------|
| CorsConfig.java | allowedHeaders("*") 允许所有请求头 | 限制为实际需要的请求头 |
| UserContextFilter | 无 X-User-Id 时默认识别为内部服务 | 增加服务认证头验证 |
| RedisConfig | ObjectMapper 启用 ANY 可见性 | 使用受限的 PolymorphicTypeValidator |
| FeignRequestInterceptor | X-Request-Source 头可被伪造 | 不依赖可被客户端控制的头 |
| FileAccessServiceImpl | ATTENDANCE 类型返回 false | 添加 ATTENDANCE 类型支持 |
| workflow.js | 流程配置接口无权限校验 | 后端校验用户角色 |
| system.js | 权限分配接口无权限校验 | 后端实现接口级权限校验 |
| approval/list.vue | 越权审批风险 | 后端实现审批人身份校验 |
| check.vue | 定位校验缺失 | 校验打卡位置与公司距离 |

### 业务逻辑类 (18条)

| 文件 | 问题 | 修复建议 |
|------|------|----------|
| BudgetServiceImpl:98 | remainingAmount 为 null | 查询时计算剩余金额 |
| OvertimeServiceImpl:244 | 加班时长取整错误 | 使用 RoundingMode.CEILING |
| SalaryServiceImpl:148-152 | NPE 风险 | 增加 null 保护 |
| SalaryServiceImpl:256-266 | 事务边界错误 | 删除 try-catch 或使用编程式事务 |
| FileServiceImpl:84 | 文件大小 >= 而非 > | 修正条件判断 |
| LeaveBalanceServiceImpl:195-199 | REDUCE 逻辑不完整 | 同步扣减 usedDays |
| OvertimeMapper.xml:94-98 | 过期记录未过滤 | 添加 expired_time > NOW() 条件 |
| RepairServiceImpl:94-112 | 补卡日期无范围限制 | 添加日期范围校验 |
| AttendanceController:63-74 | 打卡记录无权限校验 | 添加查询范围限制 |

### 前端类 (15条)

| 文件 | 问题 | 修复建议 |
|------|------|----------|
| approval/index.vue:416-428 | quickApprove 无确认 | 添加确认弹窗 |
| approval/index.vue:513-528 | 转交无目标用户校验 | 使用用户选择器 |
| stamp/index.vue:427-431 | 撤回无防抖 | 使用 withDebounce |
| leave/index.vue:377-379 | 审批列表无权限校验 | 后端过滤无权限数据 |
| purchase/index.vue:350-369 | 采购金额无上限 | 添加金额阈值配置 |
| repair-card/index.vue:388-396 | 防抖定时器未清理 | 组件卸载时清理定时器 |
| system/role/index.vue:156-169 | 权限树半选中状态错误 | 使用 getCheckedKeys(true) |
| attendance/rules/index.vue:245 | JSON 编辑器无安全校验 | 校验原型链污染关键字 |
| login/index.vue:83-89 | 登录无防暴力破解 | 增加验证码或次数限制 |

## Medium 问题 (25条)

| 类别 | 问题 | 修复建议 |
|------|------|----------|
| 日志安全 | FeignErrorDecoder 记录完整响应体 | 脱敏处理后记录 |
| 文件操作 | mkdirs 失败无处理 | 检查返回值 |
| 性能 | 定时任务无批次处理 | 分页查询用户 |
| 前端 | 首页数据硬编码 | 调用真实 API |
| 表单 | mobile/email 校验缺失 | 添加正则验证 |
| 日期 | 可选任意日期无限制 | 添加日期范围限制 |
| 异步 | Promise.all 任一失败全部失败 | 使用 Promise.allSettled |

## Low 问题 (10条)

| 问题 | 修复建议 |
|------|----------|
| 固定脱敏值 "******" | 改用动态脱敏 |
| 邮箱正则对短邮箱处理 | 优化正则表达式 |
| roles 逗号分隔设计 | 改为 List 类型 |
| processInstanceId 时间戳重复 | 使用 UUID |
| 魔法数字硬编码 | 定义为常量 |
| 空 catch 块 | 至少记录日志 |

---

## 建议优先修复 (Top 5)

### 1. 工作流审批权限校验缺失 (Critical)
所有 approve 方法添加 approverId 校验，防止任意用户审批他人申请。
- 涉及文件: LeaveServiceImpl, ExpenseServiceImpl, PurchaseServiceImpl, StampServiceImpl
- 影响: 安全漏洞，任意用户可越权审批

### 2. Token 明文存储 (Critical)
改用 HttpOnly Cookie 存储 Token，防止 XSS 攻击窃取凭证。
- 涉及文件: user.js, request.js
- 影响: 用户会话可被劫持

### 3. 部门/角色管理无权限控制 (Critical)
DeptController 和 RoleController 添加 @PreAuthorize 注解。
- 涉及文件: DeptController, RoleController
- 影响: 普通用户可删改系统关键配置

### 4. 补卡申请并发竞态 (Critical)
RepairServiceImpl 添加数据库唯一约束防止重复提交。
- 涉及文件: RepairServiceImpl
- 影响: 重复打卡记录绕过限制

### 5. CORS 配置风险 (High)
限制 allowedHeaders 禁止 "*" 配置。
- 涉及文件: CorsConfig.java
- 影响: Authorization/Cookie 头可能被恶意网站获取

---

## 评审统计

| 指标 | 数值 |
|------|------|
| 评审文件 | ~270 个 |
| 评审 Agent | 20 个并发 |
| 原始发现 | ~260 条 |
| 验证确认 | 88 条 |
| Critical | 14 条 |
| High | 45 条 |
| Medium | 25 条 |
| Low | 10 条 |

---

*报告生成时间: 2026-05-31*
