# SolidOA 第三轮代码评审最终报告

**评审日期**: 2026-05-30
**评审流水线**: 20 Agent 分组评审 → 每条发现独立验证 Agent → 汇总报告
**覆盖范围**: 全量源文件（~270 个文件，20 组）
**原始发现**: 123 条
**验证 Agent**: 20 组评审 + 20 个验证 Agent（按组批量验证）
**验证后确认**: 95 条真实问题，28 条误报

---

## 总体健康度

项目架构清晰、模块划分合理，但存在多个阻断性 bug 和安全隐患。经三轮评审修复后，核心问题已大幅减少，但仍有 3 个 Critical 级别的阻断性问题需要立即修复。

| 严重度 | 原始发现 | 验证后确认 | 误报 |
|--------|----------|------------|------|
| Critical | 3 | **3** | 0 |
| High | 25 | **20** | 5 |
| Medium | ~60 | **~50** | ~10 |
| Low | ~30 | **~22** | ~8 |
| **合计** | **123** | **~95** | **~28** |

---

## Critical（3 条）— 必须立即修复

| # | 文件:行号 | 问题 | 修复方案 |
|---|-----------|------|----------|
| C1 | `approval/index.vue:349` | 调用不存在的 `workflowApi.approve/transfer` 方法，**所有审批操作完全不可用** | 根据 `businessType` 动态选择 `approveLeave/approveStamp/approvePurchase` |
| C2 | `approval/index.vue:295` | 状态筛选值大小写不匹配（`'pending'` vs `'PENDING'`），**审批列表永远为空** | 将 `statusTabs` 的 value 改为大写 |
| C3 | `PurchaseServiceImpl.java:139` | 引用不存在的字段名 `purchaseProgressMapper`（实际声明为 `progressMapper`），**编译失败** | 改为 `progressMapper` |

---

## High（20 条）— 本周内修复

### 前端（9 条）

| # | 文件:行号 | 问题 | 修复方案 |
|---|-----------|------|----------|
| H1 | `layout/index.vue:7` | 移动端侧边栏展开/收起逻辑反转 | `:class='{ collapsed: !isMobileMenuOpen }'` |
| H2 | `leave/index.vue:355` | 半天请假时长计算 bug（PM-PM 同日=1.5天） | 修正 endHalf 映射和计算公式 |
| H3 | `approval/index.vue:249` | `statusTabs` 为普通数组，count 更新不触发视图 | 改为 `ref()` 或 `reactive()` |
| H4 | `approval/index.vue:449` | `getExpenseFlow/getOvertimeFlow` 方法不存在 | 补充 API 方法 |
| H5 | `collab/message/index.vue:150` | `handleExpand` 仅首次更新详情面板 | 改为无条件赋值 `selectedMessageId` |
| H6 | `purchase/index.vue:371` | 提交时未过滤空商品名明细项 | 只提交 `validItems` |
| H7 | `userStore.js:51` | `getUserInfo` 失败时未清除 `access_token/refreshToken` | catch 块补充清除 |
| H8 | `request.js:21` | `SENSITIVE_PATTERNS` 正则过于宽泛 | 改为精确匹配模式 |
| H9 | `repair-card/index.vue:432` | 待审批数据取值结构错误 | 统一取值逻辑 |

### 后端（11 条）

| # | 文件:行号 | 问题 | 修复方案 |
|---|-----------|------|----------|
| H10 | `ReportServiceImpl.java:54` | `@Async` 自调用失效，导出同步阻塞 | 注入自身或提取到独立类 |
| H11 | `ReportServiceImpl.java:136` | 循环内 `workbook.dispose()` 导致后续写入失败 | 移除循环内 dispose |
| H12 | `LogTraceConfig.java:37` | `TraceIdServletFilter` 双重注册 | 移除 `@Component` |
| H13 | `ApprovalNodeServiceImpl.java:130` | 审批拒绝后未更新业务实体状态 | REJECTED 分支更新对应实体 |
| H14 | `StampServiceImpl.java:256` | `generateStampNo` 竞态条件 | 使用 `FOR UPDATE` 或数据库序列 |
| H15 | `ReminderServiceImpl.java:45` | Redis 计数器超限后不回滚 | 超限后 `decrement` 回滚 |
| H16 | `BudgetServiceImpl.java:107` | `adjust()` 调减未校验负数 | 添加负数校验 |
| H17 | `FileServiceImpl.java:149` | `list()` 分页参数完全无效 | 使用 `Page` 分页 |
| H18 | `RepairServiceImpl.java:66` | 分页查询实际全量加载 | 使用 `Page` 分页 |
| H19 | `ApprovalRecordServiceImpl.java:171` | 审批统计金额全表加载求和 | 使用 SQL `SUM` |
| H20 | `PurchaseServiceImpl.java:295` | 采购统计全表加载聚合 | 使用 SQL `GROUP BY` |

---

## Medium（约 50 条）— 近期规划修复

### 安全与配置（8 条）
- `RedisConfig.java:40` — `java.lang.*` 白名单存在 RCE 风险
- `RateLimiterFilter.java:117` — Redis 异常时 fail-open 全放行
- `CorsConfig.java:18` — CORS 允许所有来源
- `WorkflowMessageProducer.java:148` — 成功时 retryCount 仍递增
- `WorkflowMessageProducer.java:88` — `sendReminderMessage` 缺 `@Transactional`
- `ApprovalRecordController.java:114` — exportRecord 缺权限校验
- `StampController.java:78` — 用印类型与 Service 枚举不一致
- `ReminderServiceImpl.java:57` — approverId 字段语义错误

### 业务逻辑（12 条）
- `GoOutServiceImpl.java:79` — approve 未校验当前状态
- `GoOutServiceImpl.java:86` — approve 未记录审批人
- `OvertimeServiceImpl.java:58` — `OvertimeType.valueOf` 未捕获异常
- `RoleServiceImpl.java:92` — 有权限数据时 Redis 缓存无 TTL
- `RoleServiceImpl.java:78` — delete 未校验角色是否存在
- `RoleServiceImpl.java:115` — assignPermissions 逐条 insert N+1
- `BudgetServiceImpl.java:58` — list() 全表加载后内存过滤
- `AuditLogServiceImpl.java:54` — `wrapper.last()` 拼接分页参数
- `ApprovalFlowServiceImpl.java:111` — setDefault 未校验 businessType
- `StampServiceImpl.java:86` — insert 后无修改地 updateById
- `ReliableMessageService.java:46` — saveToOutbox 为空实现
- `WorkflowMessageConsumer.java:178` — extractVersion 偏移量错误

### 类型安全与一致性（10 条）
- `PurchaseItemForm.java:32` — 缺少 `@DecimalMin` import
- `LeaveForm.java:21` — String 字段用 `@NotNull` 应为 `@NotBlank`
- `PurchaseMapper.java:18` — `selectMaxPurchaseNo` 缺 `@Param`
- `ApprovalCcMapper.java:48` — count 方法返回 int 应为 long
- `LeaveDTO.java:16` — days 使用 Double 精度风险
- `Purchase.java:26` — deptId/requesterDeptId 语义冗余
- `SalaryServiceImpl.java:118` — 日期解析缺空字符串防护
- `ApprovalNodeDetailMapper.xml:48` — 缺状态过滤条件
- `ReminderRecordMapper.xml:9` — `DATE()` 函数导致索引失效
- `RoleController.java:56` — assignPermissions 用 Map 非 Form

### 前端（10 条）
- `dashboard/index.vue:4` — 仪表盘重复 Header
- `layout/index.vue:157` — adminOnly 菜单未做权限过滤
- `attendance/check/index.vue:216` — 日期硬编码 31 天
- `system/dept/index.vue:78` — 部门管理全部桩代码
- `login/index.vue:85` — 「记住我」未生效
- `expense/index.vue:661` — dangerouslyUseHTMLString 未转义
- `stamp/index.vue:428` — handleCancel 缺 try-catch
- `purchase/index.vue:371` — 提交未过滤空明细
- `request.js:133` — 端口脱敏正则误伤 URL
- `workflow.js:35` — getMyProcessed 无分页参数

---

## Low（约 22 条）— 择机修复

- `TraceIdFilter.java:40` — TraceId 未写入响应头
- `MetricsCollector.java:123` — SLF4J Python 风格占位符
- `MetricsCollector.java:134` — resetMetrics 数据不一致
- `IdempotentService.java:76` — releaseLock 无归属校验
- `BudgetForm.java:1` / `SalaryForm.java:1` — 缺校验注解
- `SummaryServiceImpl.java:95` — getExceptions 空壳实现
- `RepairServiceImpl.java:81` — 待审批列表硬编码 100 条
- `ReportServiceImpl.java:25` — 导出目录 Linux 路径
- `FileServiceImpl.java:255` — delete() uploaderId NPE
- `FileServiceImpl.java:120` — 文件流重复读取
- `FileVO.java:10` — 暴露内部存储路径
- `FileServiceImpl.java:64` — MinIO 凭据未使用
- `MagicNumberUtil.java:27` — docx/zip 共享 magic number
- `RabbitMQConfig.java:83` — mandatory 未设置
- `SalaryServiceImpl.java:220` — batchPay 部分失败静默
- `OvertimeForm.java:7` — 未使用 BigDecimal 导入
- `GoOut.java:27` — outDate/startTime 语义冗余
- `OvertimeForm.java:16` — @AssertTrue 语义不一致
- `LeaveController.java:47` — approve 用 Map 非 DTO
- `TaskController.java:31` — parseInt 无 try-catch
- `MessageOutboxMapper.java:15` — SELECT * 含 TEXT 列
- `UserForm.java:11` — realName 缺校验注解

---

## 建议优先修复 Top 5

| 优先级 | 问题 | 理由 |
|--------|------|------|
| **1** | `approval/index.vue:349` 审批方法不存在 | **核心业务完全阻断**，所有审批操作无法执行 |
| **2** | `approval/index.vue:295` 状态筛选大小写不匹配 | **审批列表永远为空**，用户无法查看任何待办 |
| **3** | `PurchaseServiceImpl.java:139` 字段名拼写错误 | **编译失败**，采购模块完全不可用 |
| **4** | `ApprovalNodeServiceImpl.java:130` 审批拒绝未更新状态 | **业务数据不一致**，拒绝后实体停留在 PENDING |
| **5** | `StampServiceImpl.java:256` 印章编号竞态条件 | **数据完整性风险**，并发下可能生成重复编号 |

---

## 误报分析（28 条）

主要误报原因：
1. **文件不存在**（7 条）：G13 的 `approval/list.vue`、G16 的 `attendance/groups/shifts/leave-types/index.vue` 等文件路径在第三轮修复中已变更
2. **VO 分层保护**（3 条）：`User.java` 的 `@JsonIgnore` 问题因 VO 分层模式实际不泄露密码
3. **已做防护**（5 条）：`ExpenseServiceImpl` 的状态校验由 WHERE 条件隐含、`DeptService` 的 deleted 过滤已在 SQL 中
4. **误判代码**（8 条）：`Connection` 图标实际在使用、`GoOutForm` 的 `@NotBlank` 实际存在、`calcTotalAmount` 在模板中有引用
5. **影响有限**（5 条）：MD5 截断碰撞概率可忽略、`requestPerSecond` 命名有误导但功能正确

---

**评审结论**：优先修复 3 个 Critical 和 Top 5 High 级别问题，确保核心审批流程可用且编译通过。Medium 和 Low 级别问题纳入后续迭代。

---

*报告由 20 个并发评审 Agent + 20 个独立验证 Agent 生成，经汇总 Agent 合并产出。*
