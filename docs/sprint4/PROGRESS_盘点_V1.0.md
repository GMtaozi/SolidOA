# SolidOA Sprint 4-5 完整进度盘点

> 截止：2026-06-02
> 目的：明天按本文档接续修复
> 文档范围：V2.0 SSD 全部 18 章 + Sprint 4 收尾项

---

## 📊 总览

| 状态 | 章节数 | 占比 |
|---|---|---|
| ✅ 完全完成 | 10 | 56% |
| 🟡 部分完成 | 5 | 28% |
| 🔴 完全未做 | 3 | 16% |
| **合计** | **18** | **100%** |

**整体完成度：~78%**（静态代码层面 100%，运行时未验证）

---

## ✅ 已完全完成（10 项）

### Sprint 4 收尾
- ✅ **A1 第二步**：4 业务 ServiceImpl 委托 UniversalApproval（Leave/Expense/Stamp/Purchase）
- ✅ **A1 前置**：UniversalApprovalServiceImpl 乐观锁守卫（MyBatis-Plus @Version 自动）
- ✅ **B1 真实通知**：WorkflowMessageConsumer Feign 调 MessageClient 写 oa_message
- ✅ **B1 真实通知**：催办消息同样真实写入
- ✅ **D1 SQL**：wf_message_outbox 表（13 字段 4 索引）已 docker exec 执行
- ✅ **B1 编译错误修复**：WorkflowMessageConsumer.java:174-175 少打 `)` + 多一行 log
- ✅ **ReminderServiceImpl 第 3 处 TODO**

### V2.0 章节
- ✅ **第 2 章 设计系统**：tokens.scss + mixins.scss + element-overrides.scss
- ✅ **第 3 章 通用组件库**：16 个 OaXxx 组件（OaButton/Card/Dialog/FormDialog/StatusBadge/Table/ApprovalCard/ApprovalFlow/Empty/Pagination/SearchForm/PageHeader/Icon + 3 个原有）
- ✅ **第 5 章 系统管理**：DDL 执行（V2.0__permission_upgrade.sql + data_scope 修复）+ OaFormDialog 3 页改造 + DictService 字典缓存（@PostConstruct warmupCache + Redis 1h TTL）+ OperLog @Async 异步
- ✅ **第 6 章 审批流**：9 状态/8 事件/13 transitions 状态机 + UniversalApprovalService + timeline 端点（GET /approval/{type}/{id}/timeline）
- ✅ **第 10 章 用印去重**：验证 system 下无 Stamp 重复文件 + 前端 stamp-manage 走 workflow API
- ✅ **第 15 章 缓存与可观测**：CacheConfig 5 域（user/dept/dict/contact/schedule/dashboard）+ TraceId 4 件套（Filter/Interceptor/FeignInterceptor/logback pattern）+ Prometheus + Grafana + 4 接口 @Cacheable
- ✅ **第 16 章 数据库规范**：16.1 字符集统一（5 库 64 表 utf8mb4_unicode_ci）

### 工程化
- ✅ **CI 接入 + 主动删除**：你判定不需要 CI，删除 .github/workflows
- ✅ **vite.config.js ESM 兼容**：__dirname → import.meta.url
- ✅ **4 道闸验证（4 次）**：每次 Sprint 完成都跑全模块编译 + type-check + lint + test + build

---

## 🟡 部分完成（5 项）

### 1. 第 1 章 总体架构 🟡
**已完成**：
- ✅ 顶层 solidoa-common/ + solidoa-dingtalk/ 旧目录清理（之前 commit）
- ✅ Seata server 容器（seataio/seata-server:1.5.2 + Nacos 注册中心 + file 模式 + 端口 8091）
- ✅ Sentinel dashboard 容器（bladex/sentinel-dashboard:1.8.6 + 端口 8088 + 8719）
- ✅ Sentinel workflow 模块集成（feign.sentinel.enabled + eager: true）
- ✅ 6 条限流规则（workflow-approve/reject/transfer + auth-login + file-upload + message-send）

**未完成（明天优先）**：
- ❌ **Seata AT 业务集成**：`@GlobalTransactional` 在请假扣减余额/报销冻结预算方法上未加
- ❌ **Sentinel @SentinelResource 注解**：核心 Controller 端点未加 resource 注解
- ❌ **Seata undo_log 表**：业务库需加（5 个库）
- ❌ **Sentinel 规则动态加载**：当前是本地文件，未通过 Nacos 配置中心动态下发
- ❌ **Maven 聚合根合并**：solidoa-parent 是唯一聚合根（已 OK），Seata/Sentinel 集成到 solidoa-parent 的 dependencyManagement

**预估**：2-3 天

### 2. 第 4 章 工程化 🟡
**已完成**：
- ✅ tsconfig.json + ESLint + Prettier + Vitest 框架
- ✅ 前端类型检查通过（0 错误）
- ✅ 5 文件 28 测试用例

**未完成**：
- ❌ **100% .js → .ts 迁移**：API 层 / composables / store 仍有 .js
- ❌ **197 个 lint 警告**：`vue/attributes-order` 等历史遗留（未用 --fix 批量处理）
- ❌ **GitHub Actions CI**（你主动决定删除）
- ❌ **Vitest 覆盖率 ≥ 60%**：当前 5 测试文件覆盖率未知

**预估**：3-5 天

### 3. 第 9 章 通讯录/日程/消息 🟡
**已完成**：
- ✅ 通讯录 + 日程 已有基础功能
- ✅ 消息中心：MessageService + MessageMapper + MessageController（V1.x 已建）
- ✅ 5.2 Sprint 已加 dict 缓存
- ✅ WebSocket MVP：WebSocketConfig + WebSocketSessionManager + /ws/notification 端点
- ✅ spring-boot-starter-websocket 依赖

**未完成**：
- ❌ **WebSocket 与 workflow MQ 联动**：审批通过/拒绝触发实时推送
- ❌ **Redis pub/sub 跨服务广播**：当前 WebSocket 只在 system 进程内有效
- ❌ **前端铃铛组件**：订阅 /ws/notification 显示未读数
- ❌ **8 个消息端点全实现**：当前 3 个（unreadCount/markAsRead/sendMessage）
- ❌ **周期性日程 DAILY/WEEKLY/MONTHLY**

**预估**：4-5 天

### 4. 第 17 章 部署运维 🟡
**已完成**：
- ✅ Docker Compose 6 个核心服务（mysql/redis/nacos/rabbitmq/minio/gateway/system/workflow/hr/file/dingtalk）
- ✅ Prometheus + Grafana（15 章加）
- ✅ Sentinel dashboard + Seata server（1 章加）

**未完成**：
- ❌ **新 4 服务 compose**：solidoa-notice / solidoa-asset / solidoa-knowledge / solidoa-collaboration（V2.0 第 1 章目标，但服务还没建）
- ❌ **镜像推送脚本**：阿里云/腾讯云镜像仓库
- ❌ **健康检查端点**：所有 service 的 /actuator/health
- ❌ **start-all.bat 启动脚本**：V1.x 已有，需适配新容器

**预估**：2-3 天

### 5. 第 18 章 实施路线 🟡
- ✅ V2.0 优化蓝图文档本身已写（1719 行）
- ❌ 实施路线 + 验收标准动态更新（每章完成时未同步到文档）

**预估**：1 小时（文档维护）

---

## 🔴 完全未做（3 项）

### 1. 第 7 章 考勤模块扩展 🟠 扩展
**未做**：
- ❌ `<oa-attendance-calendar>` 日历视图组件
- ❌ 月报 PDF + Excel 导出
- ❌ 异常告警走消息中心 + 钉钉
- ❌ 考勤规则页用 OaTable + OaFormDialog 改造（5 个页面）

**预估**：4-5 天

### 2. 第 8 章 财务/工资补齐 🟠 扩展
**未做**（最难的章节之一）：
- ❌ **个税累计预扣法**：税率表 + TaxCalculator 服务
- ❌ `oa_tax_deduction` 表（5 库都没建）
- ❌ `MonthEndService` 月结事务（跨表汇总 + 归档）
- ❌ 银行对账接口（占位即可）
- ❌ 工资条 / 工资单生成

**预估**：5-7 天

### 3. 第 14 章 钉钉集成重构 🟡 重构
**未做**（按你"放最后"原则）：
- ❌ 顶层 solidoa-dingtalk/ 旧目录（已不存在，但 V2.0 文档要求彻底重构）
- ❌ State 走 Redis（现在是 ConcurrentHashMap 内存）
- ❌ @Async 替换 new Thread
- ❌ 钉钉审批回调走 workflow 引擎
- ❌ 通讯录定时同步
- ❌ 钉钉企业应用配置（CorpId/CorpSecret 凭证）

**预估**：3-4 天

---

## 📋 后端残留 3 个真实 TODO

| 文件 | 行 | TODO 内容 | 归属章节 |
|---|---|---|---|
| `AttendanceRuleServiceImpl.java` | 461 | 调用户服务获取所有在职员工 | 第 7 章 |
| `AttendanceServiceImpl.java` | 198 | 钉钉数据合并逻辑 | 第 14 章 |
| `LeaveServiceImpl.java` | 70 | Camunda 工作流引擎集成 | P3 长期（V2.0 之外）|

---

## 🎯 明天 Sprint 5 推荐顺序

按"价值/工作量"比 + 阻塞关系排序：

| 顺序 | 任务 | 工作量 | 阻塞 |
|---|---|---|---|
| 1 | **第 1 章 业务 Seata AT 集成**（请假扣减余额） | 1-2 天 | 需新建 BalanceService |
| 2 | **第 1 章 业务 Sentinel @SentinelResource 注解** | 0.5 天 | 无 |
| 3 | **第 9 章 WebSocket 与 MQ 联动 + 前端铃铛** | 1-2 天 | 需 MessageClient 已有 |
| 4 | **第 7 章 考勤月报导出 + 日历组件** | 4-5 天 | 无 |
| 5 | **第 8 章 财务个税预扣 + MonthEndService** | 5-7 天 | 需数据库 + 算法 |
| 6 | **第 17 章 新 4 服务 compose + 镜像脚本** | 2-3 天 | 需先建 4 个服务 |
| 7 | **第 4 章 .js → .ts 100% 迁移** | 5-8 天 | 无 |
| 8 | **第 14 章 钉钉集成重构** | 3-4 天 | 需 CorpId 凭证 |

**总估时**：22-32 天（一个完整 Sprint 周期）

---

## 📁 文档维护提醒

每章完成时，**必须更新**：
1. `docs/ssd/SSD_V2.0_Optimization.md` 验收标准 checkbox `[x]`
2. 本文档状态从 🔴 → 🟡 → ✅
3. git commit 信息含章节号

**当前 SSD V2.0 文档尚未更新任何 checkbox**（Sprint 期间未同步）。明天建议先做一次文档同步（约 1 小时），保证文档与代码一致。

---

## 📦 完整提交链（本次会话）

```
489fd24  feat: Sprint 4 - 状态机统一 + 真实通知 + D1 + CI 4道闸
a83b7c6  fix: vite.config.js 用 import.meta.url 替代 __dirname
ebad329  debug: build 步骤输出日志
9943436  chore: 移除 CI debug 步骤
fdcf8b6  chore: 移除 GitHub Actions CI 配置
3fde5d4  feat: 第 5 章系统管理升级 - DDL/OaFormDialog/字典缓存/操作日志
3419c91  fix: 修正 V2.0 SQL 角色 data_scope 回填
f3fab4d  feat: 第 15 章缓存与可观测 - CacheConfig + TraceId + Prometheus
39bf7b6  feat: 第 3/6/9/15 章补全 + 第 1 章验证
a348382  feat: 第 1 章 Seata + Sentinel 集成
```

**共 10 个 commit，全部已推送到 GitHub**

---

## ⚠️ 诚实声明（待验证项）

按 Karpathy 诚实原则，**以下未在运行时验证**：
- ❓ 各服务启动后能否正常连 Nacos / Redis / MySQL
- ❓ WebSocket 握手是否正常
- ❓ Sentinel dashboard 能否拉到 workflow 服务
- ❓ Seata TC 能否注册到 Nacos
- ❓ Prometheus 端点是否暴露正确
- ❓ Redis 缓存反序列化是否成功（Dict 缓存 / Contact @Cacheable）
- ❓ Feign 调下游是否带 X-Trace-Id

**建议明天第一件事**：先 `docker-compose up -d` 启服务，挨个冒烟测试。否则 78% 完成度可能只是"代码完整度"。

---

**下次接续入口**：本文档 `/docs/sprint5/PROGRESS_盘点_V1.0.md` （明天建议移到独立目录）
