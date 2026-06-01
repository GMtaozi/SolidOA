# SolidOA 代码评审报告

**评审日期**: 2026-05-29
**评审方式**: 20 个并发 Agent 分组评审 + 对抗式验证
**覆盖文件**: ~270 个源文件（后端 Java + 前端 Vue3/uni-app）
**原始发现**: 148 条 → 验证后确认: 136 条真实问题

---

## 总体健康度

**中等偏差**。项目架构清晰、模块划分合理，但存在多个 Critical 和 High 级别的安全漏洞、逻辑 bug 和数据一致性问题，需要优先修复。前端存在 XSS 漏洞和 Vue3 兼容性问题，后端存在身份伪造、缓存穿透、消息丢失等风险。

| 严重度 | 数量 |
|--------|------|
| Critical | 6 |
| High | 24 |
| Medium | 58 |
| Low | 48 |

---

## Critical（6 条）— 必须立即修复

### C1. Redis 反序列化 RCE 风险
- **文件**: `solidoa-common/.../config/RedisConfig.java:36`
- **问题**: 使用 `LaissezFaireSubTypeValidator` 配合 `DefaultTyping.NON_FINAL`，对所有类的反序列化不做限制。攻击者可向 Redis 写入恶意 JSON 触发远程代码执行。
- **修复**: 替换为 `BasicPolymorphicTypeValidator` 白名单模式，仅允许 `com.solidoa.*`、`java.util.*`、`java.time.*`。

### C2. Feign 幂等键使用 UUID，完全失去幂等语义
- **文件**: `solidoa-common/.../config/FeignIdempotentInterceptor.java:53`
- **问题**: `generateIdempotentKey` 使用 `UUID.randomUUID()`，每次 Feign 调用（含重试）产生不同键，无法防止重复执行。
- **修复**: 基于请求的业务语义确定性生成：`method + url + md5(body)`。

### C3. 日志脱敏完全失效
- **文件**: `solidoa-common/.../config/LogMaskFilter.java:18`
- **问题**: `LogMaskUtil.mask(message)` 的结果 `maskedMessage` 从未被回写到日志事件，敏感信息仍以明文输出。
- **修复**: 移除无效 Filter，改用 Logback `MessageConverter` 或自定义 `PatternLayout` 在输出层脱敏。

### C4. 角色权限分配为空操作
- **文件**: `solidoa-system/.../service/impl/RoleServiceImpl.java:93`
- **问题**: `assignPermissions` 仅删除 Redis 缓存并打印日志，未执行任何数据库写入，角色权限不会发生任何变化。
- **修复**: 先删除角色原有权限关联，再批量插入新关联，最后清除缓存。

### C5. 报销审批状态永远不会变更
- **文件**: `solidoa-workflow/.../service/impl/ExpenseServiceImpl.java:92`
- **问题**: `approve()` 计算了 `newStatus` 但从未调用 `expense.setStatus(newStatus)`，update 语句将 status 写回 PENDING，报销单永远停留在待审批状态。
- **修复**: 在计算 newStatus 后添加 `expense.setStatus(newStatus)`。

### C6. 盲目信任请求头，存在身份伪造风险
- **文件**: `solidoa-common/.../security/UserContextFilter.java:23`
- **问题**: 直接从 `X-User-Id`、`X-User-Roles` 请求头读取用户信息并标记为 verified=true。绕过网关直接访问微服务端口即可冒充任意用户。
- **修复**: 增加网关签名验证（HMAC）或 mTLS，区分内外部请求。

---

## High（24 条）— 本周内修复

### 安全类

| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H1 | `gateway/.../JwtSecretValidator.java:28` | JWT Secret 长度不足仅 warn 不阻断启动，弱密钥可被暴力破解 | 改为 `throw IllegalStateException` 阻断启动 |
| H2 | `common/.../FeignRequestInterceptor.java:40` | 透传 X-User-* 头无安全校验，可伪造用户身份 | 网关层剥离并重新注入，下游校验一致性 |
| H3 | `common/.../security/SecurityValidator.java:55` | 角色校验用 `contains` 子串匹配，`ADMIN_ASSISTANT` 也会通过 `ADMIN` 校验 | 改为精确匹配 `Set.contains()` |
| H4 | `common/.../security/UserContextFilter.java:23` | 微服务端口被直接访问时可伪造 X-User-* 头冒充任意用户 | 增加网关签名验证 |
| H5 | `workflow/.../controller/ApprovalRecordController.java:71` | `/record/all` 端点无权限校验，任意用户可查询全部审批记录 | 增加角色校验或网关拦截 |
| H6 | `web/.../views/workflow/leave/index.vue:411` | `dangerouslyUseHTMLString` 拼接用户输入，存在 XSS | 移除或做 HTML 实体转义 |
| H7 | `web/.../views/workflow/purchase/index.vue:305` | 同上 XSS 问题 | 同上 |

### 逻辑 Bug 类

| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H8 | `gateway/.../RateLimiterFilter.java:146` | `extractPathKey` 路径截取错误，所有超 3 段路径归入同一限流桶 | 过滤空段后截取前 3 段 |
| H9 | `workflow/.../entity/TransferRecord.java:10` | 缺少 `@TableId` 注解，MyBatis-Plus insert 无法回填 id | 添加 `@TableId(type = IdType.AUTO)` |
| H10 | `workflow/.../entity/ReminderRecord.java:10` | 同上缺少 `@TableId` | 同上 |
| H11 | `workflow/.../listener/WorkflowMessageConsumer.java:36` | MANUAL ACK 模式未调用 `basicAck`，消息无限重投 | 添加 Channel 参数，成功时 ack，失败时 nack |
| H12 | `workflow/.../controller/TaskController.java:20` | `getUserId` 未做 null 检查，缺少 X-User-Id 头时 NPE | 判空后抛 BusinessException |
| H13 | `workflow/.../controller/ReminderController.java:17` | 同上 NPE 风险 | 同上 |
| H14 | `workflow/.../controller/LeaveController.java:64` | `addSign/transfer` 对 body 字段无 null 防护，NPE | 使用 Form 对象 + @Valid |
| H15 | `system/.../controller/RoleController.java:54` | `assignPermissions` 缺少空值防御，NPE | null 检查或改用 Form 对象 |
| H16 | `workflow/.../service/impl/ApprovalCcServiceImpl.java:63` | `createCcRecords` 吞没异常导致数据不一致 | 重新抛出异常 |
| H17 | `workflow/.../service/impl/ApprovalRecordServiceImpl.java:321` | `generateRecordNo` 内存计数器重启后编号重复 | 改用数据库序列 |
| H18 | `workflow/.../service/impl/LeaveServiceImpl.java:44` | `AtomicLong` 计数器重启归零导致单号重复 | 改用 Redis INCR 或数据库序列 |
| H19 | `hr/.../service/impl/GoOutServiceImpl.java:48` | 分页失效，全量加载后仅截取页面 | 使用 MyBatis-Plus Page 对象 |
| H20 | `hr/.../service/impl/SummaryServiceImpl.java:180` | `getAllUserIds` 全表扫描仅提取去重 userId | 改用 `SELECT DISTINCT user_id` |
| H21 | `web/.../stores/user.js:47` | `getUserInfo` 将 `{code, data}` 整体赋值而非实际用户数据 | 改为 `res.data.data` |
| H22 | `web/.../views/finance/expense/index.vue:20` | Vue 2 过滤器语法在 Vue 3 中不生效 | 改用方法调用 |

### 性能类

| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H23 | `workflow/.../service/impl/TaskServiceImpl.java:33` | 三个分页方法全量加载到内存再手动分页 | 分页下沉到 SQL 层 |
| H24 | `system/.../service/impl/RoleServiceImpl.java:84` | `getPermissions` 空结果未缓存导致缓存穿透 | 空结果也写入缓存 |

---

## Medium（58 条）— 近期规划修复

### 安全与配置（10 条）
- `gateway/TraceIdFilter.java:27` — 直接信任客户端 TraceId 可被伪造
- `common/FeignErrorDecoder.java:38` — Result 反序列化使用原始类型
- `common/FeignErrorDecoder.java:36` — 字符串匹配判断 JSON 结构不严谨
- `common/CommonConfig.java:14` — @EnableRetry 全局启用可能导致 POST 重试
- `workflow/ApprovalRecordController.java:71` — /all 端点无权限校验
- `app/login/index.vue:33` — 登录页硬编码默认账号密码
- `web/upload.js:37` — deleteFile 路径遍历注入风险
- `web/request.js:239` — 401 直接修改 window.location 绕过 Vue Router
- `web/stores/user.js:58` — logout 未清除 access_token
- `workflow/mq/WorkflowMessageProducer.java:110` — retryFailedMessages 单事务包裹整批

### 逻辑与数据（20 条）
- `system/entity/UserRole.java:7` — 联合主键设计不当，可重复分配角色
- `system/dto/UserDTO.java:9` — DTO 含明文密码字段
- `system/entity/User.java:14` — 密码字段缺少 @JsonIgnore
- `system/service/impl/DepartmentServiceImpl.java:102` — 删除部门未校验是否有用户
- `system/service/impl/RoleServiceImpl.java:71` — 删除角色未校验是否被用户引用
- `system/service/impl/DictServiceImpl.java:31` — getTypes 硬编码返回值
- `workflow/service/impl/LeaveServiceImpl.java:273` — 循环审批检测逻辑有缺陷
- `workflow/service/impl/ExpenseServiceImpl.java:48` — 报销单号 currentTimeMillis 并发不唯一
- `workflow/service/impl/ReminderServiceImpl.java:40` — Redis increment+expire 非原子
- `workflow/service/impl/ApprovalCcServiceImpl.java:69` — getMyCcList 分页参数未使用
- `workflow/service/impl/ApprovalRecordServiceImpl.java:258` — getMyCc 内存分页
- `workflow/service/impl/ApprovalFlowServiceImpl.java:124` — clearDefault N+1 更新
- `workflow/mq/WorkflowMessageConsumer.java:125` — 手工字符串截取解析 JSON
- `hr/service/impl/AttendanceServiceImpl.java:141` — records.size()/2 计算出勤天数
- `hr/service/impl/OvertimeServiceImpl.java:242` — 加班单号仅依赖时间戳
- `hr/controller/AttendanceController.java:63` — userId 和 headerUserId 可同时为 null
- `common/config/IdempotentService.java:32` — isExecuted+markExecuted 非原子
- `common/metrics/MetricsCollector.java:135` — resetMetrics 非原子清空
- `common/config/LogTraceConfig.java:37` — Filter 重复注册
- `app/api/index.js:8` — GET 请求参数多层包装导致查询串错误

### 类型安全与一致性（15 条）
- `workflow/entity/Purchase.java:99` — createTime/updateTime 应为 LocalDateTime
- `workflow/vo/PurchaseVO.java:52` — 同上
- `workflow/vo/TaskVO.java:15` — applyTime 使用 String 而非日期类型
- `common/vo/LeaveDTO.java:13` — startDate/endDate 使用 String
- `workflow/form/PurchaseItemForm.java:25` — @DecimalMin 作用于 Integer 不生效
- `workflow/vo/ApprovalRecordDetailVO.java:13` — content 使用 Object 类型
- `workflow/form/LeaveForm.java:28` — days 缺少最小值校验
- `workflow/form/LeaveForm.java:24` — startDate/endDate 缺少交叉校验
- `workflow/form/ApprovalForm.java:14` — approveType 缺少枚举值校验
- `workflow/mapper/ApprovalRecordMapper.java:37` — 日期参数使用 String
- `hr/attendance/service/AttendanceService.java:33` — 返回原始 Map 类型
- `hr/attendance/form/CheckForm.java:7` — 打卡表单缺少全部校验
- `hr/attendance/form/AdjustLeaveBalanceForm.java:10` — 假期余额调整缺少校验
- `hr/attendance/form/BusinessTripForm.java:13` — 出差日期范围未校验
- `hr/attendance/form/OvertimeForm.java:13` — 加班时间范围未校验

### 前端（13 条）
- `web/composables/useTime.js:75` — currentTime 计算属性不响应定时器
- `web/composables/useTime.js:7` — HMR 下 refCount 不重置导致定时器泄漏
- `web/router/index.js:57` — 未登录重定向丢失原始目标路径
- `web/views/home/index.vue:8` — 首页统计数据全部硬编码
- `web/views/workflow/leave/index.vue:464` — handleSubmit 缺少重复提交防护
- `web/views/system/user/index.vue:167` — formRef.validate() 未捕获 rejection
- `workflow/mapper/LeaveMapper.java:25` — businessType 参数未在 SQL 中使用
- `workflow/mapper/ExpenseMapper.java:21` — 同上
- `workflow/mapper/MessageOutboxMapper.java:15` — selectPendingMessages 无并发保护
- `hr/attendance/mapper/OvertimeMapper.java:42` — 跨实体查询职责混乱
- `hr/attendance/mapper/RepairCardMapper.java:15` — 分页查询缺少 count 方法
- `hr/attendance/mapper/RepairCardMapper.java:21` — 日期参数使用 String
- `hr/service/AttendanceService.java:3` — 未使用的导入

---

## Low（48 条）— 择机修复

- 字段注入 @Autowired 不符合 Spring 推荐实践（3 个 Controller）
- Role/Permission 缺少 @TableLogic deleted 字段
- 多个 Form 缺少校验注解（DeptForm、RoleForm、UserForm）
- 未使用的 import（5 处）
- 重复导入（2 处）
- RoleServiceImpl 未使用的 DICT_CACHE_KEY 常量
- RabbitMQ reminderQueue 未配置死信机制
- RabbitMQ 使用 System.err.println 而非 Logger
- gateway TraceConstants 为死代码
- @RequiresSecurity 注解无 AOP 处理器
- SummaryServiceImpl.getExceptions 为空实现
- ExpenseForm 未使用的 @NotNull 导入
- ApprovalStatisticsVO 内联全限定类名
- AttendanceSummary 使用全限定类名
- RepairCard 注释风格不一致
- AttendanceController DingTalk 图标未导入
- 限流 X-RateLimit-Reset 值格式错误
- Lua 脚本 INCR 后未设置 TTL（极端场景）
- 限流通过后额外 Redis GET（多余网络往返）
- Purchase 实体使用 LocalDate 丢失精度
- 更多...（详见各组评审原始数据）

---

## 建议优先修复 Top 5

1. **Redis 反序列化 RCE** (C1) — 安全漏洞，可远程执行代码，影响所有微服务
2. **报销审批状态不变更** (C5) + **角色权限分配为空操作** (C4) — 核心业务逻辑失效
3. **身份伪造** (C6 + H2 + H4) — 绕过网关即可冒充任意用户，整个鉴权体系失效
4. **消息队列未 ACK** (H11) + **单号重复** (H17/H18) — 数据一致性和可靠性风险
5. **前端 XSS** (H6/H7) + **Vue3 兼容性** (H22) — 用户体验和安全风险

---

*报告由 20 个并发评审 Agent 生成，经对抗式验证后汇总。*
