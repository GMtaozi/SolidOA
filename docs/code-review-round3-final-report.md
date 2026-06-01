# SolidOA 第三轮代码评审最终报告

**评审日期**: 2026-05-30
**评审流水线**: 20 Agent 分组评审 → 对抗式验证 → 汇总报告
**覆盖范围**: 全量源文件（~270 个文件）
**原始发现**: 123 条 → 验证后确认: ~118 条真实问题

---

## 总体健康度判断

项目整体处于**中等偏下**水平，核心业务流程存在关键阻断性问题，需要优先修复后方可上线使用。

| 严重度 | 数量 | 状态 |
|--------|------|------|
| Critical | 3 | 全部已验证为真 |
| High | 25 | 25/27 已验证（2条误报已排除） |
| Medium | ~60 | 未逐条验证 |
| Low | ~30 | 未逐条验证 |

---

## Critical（3条）

**所有审批、考勤核心功能完全不可用，系统无法正常运行。**

| 文件:行号 | 问题 | 修复方案 |
|-----------|------|----------|
| `approval/index.vue:349` | 调用不存在的 `workflowApi.approve/transfer` 方法，所有审批操作完全不可用 | 根据 businessType 动态选择审批方法 |
| `approval/index.vue:295` | 状态筛选值大小写不匹配（前端小写 `pending` vs 后端大写 `PENDING`），tab 切换后列表永远为空 | 统一为大写 |
| `PurchaseServiceImpl.java:139` | 引用不存在的字段名 `purchaseProgressMapper`（实际声明为 `progressMapper`），编译失败 | 改为 `progressMapper` |

---

## High（25条）

### 前端（12条）

| 文件:行号 | 问题 | 修复方案 |
|-----------|------|----------|
| `layout/index.vue:7` | 移动端侧边栏展开/收起逻辑反转，`:class` 绑定与 CSS 逻辑相反 | 改为 `:class='{ collapsed: !isMobileMenuOpen }'` |
| `leave/index.vue:355` | 半天请假时长计算 bug（PM-PM 同日场景），公式 `diffDays + startHalf + (1 - endHalf)` 计算为 1.5 | 修正时段编号方案 |
| `attendance/groups/index.vue:269` | 编辑时禁用状态被 `\|\| 1` 错误覆盖为启用（`0 \|\| 1 === 1`） | 改为 `row.status ?? 1` |
| `attendance/shifts/index.vue:227` | 同上 `status \|\| 1` 问题 | 改为 `row.status ?? 1` |
| `attendance/leave-types/index.vue:281` | 同上 `status \|\| 1` 问题 | 改为 `row.status ?? 1` |
| `userStore.js:51` | `getUserInfo` 失败时未清除 `access_token/refreshToken`，导致登录状态残留 | 补充清除 `access_token` 和 `refreshToken` |
| `request.js:21` | `SENSITIVE_PATTERNS` 正则过于宽泛（`/key/i`、`/token/i` 等），破坏正常错误信息 | 改为更精确的匹配模式 |
| `layout/index.vue:157` | `adminOnly` 菜单分组未做权限过滤，普通用户可见管理员菜单 | 根据 roles 过滤 |
| `collab/message/index.vue:150` | `handleExpand` 仅首次更新详情面板，`selectedMessageId` 只在 falsy 时赋值 | 改为无条件赋值 |
| `approval/index.vue:249` | `statusTabs` 为普通数组，修改 count 不触发视图更新 | 改为 `ref` 或 `reactive` |
| `approval/index.vue:449` | `getExpenseFlow/getOvertimeFlow` 方法不存在 | 补充 API 方法 |
| `attendance/repair-card/index.vue:432` | 待审批数据取值结构错误 | 统一取值逻辑 |

### 后端（13条）

| 文件:行号 | 问题 | 修复方案 |
|-----------|------|----------|
| `ApprovalNodeServiceImpl.java:130` | 审批拒绝后未更新业务实体状态，仅日志 return | 根据 businessType 更新对应实体状态 |
| `ReportServiceImpl.java:54` | `@Async` 自调用失效（同类 `this` 调用绕过代理），导出同步阻塞 | 注入自身或提取到独立类 |
| `ReportServiceImpl.java:136` | 循环内 `workbook.dispose()` 导致后续写入失败（dispose 删除临时文件） | 移除循环内 dispose |
| `LogTraceConfig.java:37` | `TraceIdServletFilter` 双重注册（`@Component` + `@Bean`） | 移除 `@Component` |
| `ReminderServiceImpl.java:45` | Redis 计数器超限后不回滚，永久膨胀（increment 在检查前执行） | 超限后 decrement 回滚 |
| `RoleServiceImpl.java:92` | 有权限数据时 Redis 缓存无 TTL，永不淘汰 | 设置 30 分钟 TTL |
| `BudgetServiceImpl.java:58` | `list()` 全表加载后内存过滤（`selectList(null)`） | 使用 `LambdaQueryWrapper` |
| `BudgetServiceImpl.java:107` | `adjust()` 调减未校验负数 | 添加负数校验 |
| `FileServiceImpl.java:149` | `list()` 分页参数完全无效（SQL 无 LIMIT） | 使用 Page 分页 |
| `StampServiceImpl.java:256` | `generateStampNo` 竞态条件（查询与递增非原子） | 使用 `FOR UPDATE` 或数据库序列 |
| `RepairServiceImpl.java:66` | 分页查询实际全量加载 | 使用 Page 分页 |
| `ApprovalRecordServiceImpl.java:171` | 审批统计金额全表加载求和 | 使用 SQL `SUM` |
| `PurchaseServiceImpl.java:295` | 采购统计全表加载聚合 | 使用 SQL `GROUP BY` |
| `WorkflowMessageConsumer.java:178` | `extractVersion` 偏移量计算错误（`start += 11` 应为 12） | 使用 `prefixLen` 变量 |
| `AttendanceServiceImpl.java:150` | 迟到/早退统计按记录计数导致重复 | 按 `checkDate` 去重 |

---

## Medium（约 60 条）

包括：自动填充缺失、校验注解不完整、N+1 查询问题、`@Transactional` 缺失、错误处理不一致、前端空实现等。此级别问题不影响核心功能，但会降低代码质量和系统稳定性。

---

## Low（约 30 条）

包括：未使用的 import、死代码、命名不一致、注释风格问题等。不影响功能和性能，属于代码规范层面。

---

## 建议优先修复 Top 5

| 优先级 | 问题 | 理由 |
|--------|------|------|
| **1** | `approval/index.vue:349` 审批方法不存在 | **核心业务完全阻断**，所有审批操作无法执行 |
| **2** | `approval/index.vue:295` 状态筛选大小写不匹配 | **审批列表永远为空**，用户无法查看任何待办 |
| **3** | `ApprovalNodeServiceImpl.java:130` 审批拒绝未更新状态 | **业务数据不一致**，审批拒绝后实体状态未同步 |
| **4** | `StampServiceImpl.java:256` 印章编号竞态条件 | **数据完整性风险**，并发场景下可能生成重复编号 |
| **5** | `ReminderServiceImpl.java:45` Redis 计数器不回滚 | **内存泄漏风险**，长期运行后 Redis 内存持续膨胀 |

---

**评审结论**：建议在上线前优先修复 3 个 Critical 和 Top 5 High 级别问题，确保核心审批流程可用且数据一致。Medium 和 Low 级别问题可纳入后续迭代计划。

---

*报告由 20 个并发评审 Agent 生成，经 3 个对抗式验证 Agent 确认后汇总。*
