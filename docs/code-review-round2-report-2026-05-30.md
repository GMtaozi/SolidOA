# SolidOA 第二轮代码评审报告

**评审日期**: 2026-05-30
**评审目的**: 验证第一轮 136 个问题的修复效果，检查是否引入新问题
**评审方式**: 10 个并发 Agent 分组审查
**覆盖范围**: 已修复的关键文件（聚焦第一轮问题所在模块）

---

## 总体结论

**第一轮 136 个问题全部已修复**。第二轮发现 **61 条新问题**，其中大部分是修复过程中引入的次要问题或原本未覆盖到的深层问题。无第一轮问题的回归。

| 严重度 | 数量 | 说明 |
|--------|------|------|
| Critical | 1 | 限流过滤器顺序导致认证前流量无法被限流 |
| High | 19 | 分页逻辑、单号重复、MQ 幂等、XSS 遗漏等 |
| Medium | 29 | 自动填充缺失、校验注解、N+1 查询等 |
| Low | 12 | 代码风格、空实现、硬编码等 |

---

## Critical（1 条）

### C1. 限流过滤器顺序导致限流形同虚设
- **文件**: `gateway/.../filter/RateLimiterFilter.java:159`
- **问题**: `RateLimiterFilter.getOrder()` 返回 -90，`JwtAuthenticationFilter.getOrder()` 返回 -100。JWT 认证先于限流执行，攻击者可用无效 Token 大量请求，JWT 过滤器在限流之前返回 401，限流器永远不计数。
- **修复**: 将 RateLimiterFilter 的 order 调整为 -150（在 JWT 之前执行）。

---

## High（19 条）

### 安全（4 条）
| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H1 | `gateway/JwtAuthenticationFilter.java:35` | 网关签名密钥硬编码默认值 | 移除默认值，强制从配置注入 |
| H2 | `workflow/WorkflowMessageProducer.java:183` | generateMessageId 含 UUID，幂等检查失效 | 去掉 UUID，用业务语义确定 messageId |
| H3 | `hr/OvertimeServiceImpl.java:103` | listOvertime 未按用户过滤，水平越权 | 添加 userId 过滤条件 |
| H4 | `web/expense/index.vue:650` | dangerouslyUseHTMLString 未转义 expenseNo | 添加 escapeHtml |

### 逻辑 Bug（7 条）
| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H5 | `workflow/TaskServiceImpl.java:31` | 多数据源分页逻辑错误，合并后结果不正确 | 先全量查询再统一分页或 SQL UNION |
| H6 | `workflow/LeaveServiceImpl.java:44` | AtomicLong 计数器多实例重复 | 改用数据库查询最大序号 |
| H7 | `workflow/WorkflowMessageConsumer.java:152` | extractMessageId off-by-one 错误 | start += 14 改为 start += 13 |
| H8 | `workflow/WorkflowMessageProducer.java:43` | MQ 发送在 @Transactional 内部 | 移到事务提交后 |
| H9 | `hr/Budget.java:11` | 缺少 @TableId 主键注解 | 添加 @TableId(type = IdType.AUTO) |
| H10 | `app/request.js:25` | 401 响应未 reject Promise | 添加 reject() |
| H11 | `app/stores/user.js:23` | getUserInfo 返回硬编码数据 | 调用后端 API |

### 前端逻辑（5 条）
| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H12 | `web/router/index.js:62` | 页面刷新后 roles 为空，管理员路由被拒绝 | 异步守卫先 await getUserInfo |
| H13 | `web/expense/index.vue:632` | 审批流数据硬编码为 null | 替换为 API 调用 |
| H14 | `web/home/index.vue:227` | rgba(var(--hex)) 对十六进制值无效 | 拆分为 RGB 分量 |
| H15 | `web/expense/index.vue:595` | loadStatistics 空实现 | 补充 API 调用 |
| H16 | `hr/SummaryServiceImpl.java:80` | getMonthSummary 虚假分页 | 使用 MyBatis-Plus Page |

### 设计（2 条）
| # | 文件:行号 | 问题 | 修复 |
|---|-----------|------|------|
| H17 | `workflow/WorkflowMessageProducer.java:111` | retryFailedMessages 单事务整批 | 每条消息单独事务 |
| H18 | `workflow/PurchaseServiceImpl.java:319` | convertToVO N+1 查询 | 批量预加载关联数据 |

---

## Medium（29 条）

### 自动填充缺失（6 条）
- `Leave.java:28` — createTime/updateTime 缺 @TableField(fill)
- `Expense.java:28` — 同上
- `ReminderRecord.java:17` — createTime 缺 @TableField(fill)
- `Budget.java:21` — createTime/updateTime 缺自动填充
- `Salary.java:53` — 同上
- `BudgetServiceImpl.java:49` — BeanUtils.copyProperties 覆盖 usedAmount

### 业务逻辑（10 条）
- `LeaveServiceImpl.java:186` — addSign/transfer 缺 PENDING 状态前置校验
- `ExpenseServiceImpl.java:89` — approve 缺 PENDING 状态前置校验
- `ApprovalRecordServiceImpl.java:171` — 统计金额全量加载到内存
- `ApprovalRecordController.java:114` — exportRecord 缺用户身份校验
- `GoOutServiceImpl.java:86` — approve 未持久化审批人信息
- `AttendanceServiceImpl.java:188` — checkTime 为 null 时 NPE
- `RoleServiceImpl.java:115` — assignPermissions 逐条 insert N+1
- `PurchaseServiceImpl.java:266` — getStatistics 全量加载
- `WorkflowMessageProducer.java:81` — sendReminderMessage 缺 @Transactional
- `BudgetServiceImpl.java:58` — list 全表扫描后内存过滤

### 前端逻辑（8 条）
- `web/attendance/check/index.vue:216` — month + '-31' 短月无效日期
- `web/leave/index.vue:434` — handleApproveConfirm 缺 try-catch
- `web/purchase/index.vue:328` — 同上
- `app/index/index.vue:72` — loadData 只加载待审批数
- `app/approval/list.vue:122` — processedList 从未加载
- `app/message/index.vue:67` — handleRead 仅 console.log
- `web/leave/index.vue:28` — createTime/updateTime 缺自动填充
- `gateway/JwtAuthenticationFilter.java:66` — 手动过期检查是死代码

### 校验与安全（5 条）
- `BudgetForm.java:8` — 表单缺参数校验注解
- `SalaryForm.java:10` — 同上
- `UserContextFilter.java:19` — 网关密钥硬编码默认值
- `MetricsCollector.java:135` — resetMetrics 竞态条件
- `SummaryServiceImpl.java:92` — getExceptions 空壳实现

---

## Low（12 条）

- `RabbitMQConfig.java:75` — rabbitTemplate 直接调用 @Bean 方法
- `RoleController.java:56` — assignPermissions 用原始 Map
- `UserContextFilter.java:52` — 无 userId 标记为 INTERNAL
- `FeignRequestInterceptor.java:37` — header 追加可能重复
- `PurchaseServiceImpl.java:319` — convertToVO N+1（可接受）
- `ApprovalRecordServiceImpl.java:105` — createTime null 时 NPE
- `SalaryServiceImpl.java:58` — LocalDate.parse 未指定格式
- `app/index/index.vue:9` — 部门名称硬编码
- `app/stores/user.js:11` — login try/catch 无意义
- `app/attendance/check.vue:33` — 早退状态颜色语义不准确
- `SummaryServiceImpl.java:92` — getExceptions 空壳
- `RateLimiterFilter.java:143` — 限流粒度过粗

---

## 建议优先修复 Top 5

1. **限流过滤器顺序** (C1) — 限流完全失效
2. **MQ messageId UUID** (H2) — 幂等机制失效
3. **多数据源分页逻辑** (H5) — 分页结果错误
4. **管理员路由刷新失效** (H12) — 管理员无法访问页面
5. **单号重复** (H6) — 多实例部署数据冲突

---

*第二轮评审验证：第一轮 136 个问题已全部修复，无回归。*
*第二轮新发现 61 条问题，多数为中低严重度。*
