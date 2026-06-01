# SolidOA 系统规格说明书（SSD）

> 版本：V1.2
> 日期：2026-05-26
> 作者：SolidOA Architecture Team
> 状态：已评审

---

## 目录

1. [系统概述](#1-系统概述)
2. [技术架构设计](#2-技术架构设计)
3. [核心模块设计](#3-核心模块设计)
4. [数据架构设计](#4-数据架构设计)
5. [API设计](#5-api设计)
6. [安全设计](#6-安全设计)
7. [部署方案](#7-部署方案)
8. [监控与运维](#8-监控与运维)
9. [开发规范](#9-开发规范)
10. [开发计划](#10-开发计划)

---

## 1. 系统概述

### 1.1 系统定位

SolidOA 是一款面向中小型企业的轻量级办公自动化系统，专注于审批流程、协作办公和财务管理三大核心模块。系统采用微服务架构，支持本地私有化部署，确保数据安全可控。

**目标用户规模**：约20人
**部署方式**：本地私有化部署
**移动端支持**：iOS/Android APP + 微信小程序

### 1.2 核心业务场景

```
┌─────────────────────────────────────────────────────────┐
│                      SolidOA 核心业务                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│    ┌─────────┐     ┌─────────┐     ┌─────────┐       │
│    │审批流程 │────▶│ 协作   │────▶│ 财务   │       │
│    │         │     │ 办公   │     │ 管理   │       │
│    └─────────┘     └─────────┘     └─────────┘       │
│         │               │               │              │
│         ▼               ▼               ▼              │
│    ┌─────────────────────────────────────────┐       │
│    │         组织架构与权限管理                │       │
│    └─────────────────────────────────────────┘       │
│                          │                            │
│                          ▼                            │
│                   ┌─────────────┐                   │
│                   │  钉钉集成   │                   │
│                   └─────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

### 1.3 设计原则

| 原则 | 说明 |
|------|------|
| **简单性** | 架构简洁，优先选择轻量级方案 |
| **解耦性** | 服务间通过接口通信，降低耦合 |
| **可扩展** | 模块化设计，支持功能扩展 |
| **安全可控** | 数据本地存储，字段级加密 |
| **易于运维** | 直接部署JAR，日志清晰，排查方便 |

---

## 2. 技术架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端层                                    │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │
│  │Web管理端│  │APP(iOS) │  │APP(安卓) │  │  小程序  │  │ 钉钉端  │     │
│  │ Vue3    │  │ UniApp  │  │ UniApp   │  │ UniApp   │  │ 钉钉应用│     │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘     │
├───────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────┤
│       └─────────────┴─────────────┴─────────────┴─────────────┘         │
│                              │                                            │
│                    HTTPS / 内网WiFi                                       │
│                              │                                            │
├──────────────────────────────┼────────────────────────────────────────────┤
│                         网关层 (Gateway)                                  │
│                   Spring Cloud Gateway :8080                              │
│                   JWT认证 · 路由转发 · 限流熔断 · Sentinel                 │
├──────────────────────────────┼────────────────────────────────────────────┤
│                         服务层 (Microservices)                             │
│                                                                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────┐ │
│  │ System  │ │Workflow │ │ Colla   │ │ Finance │ │Attendance│ │  File │ │
│  │ :8081   │ │ :8082   │ │ :8083   │ │ :8084   │ │ :8085   │ │:8086  │ │
│  │ 用户    │ │ Camunda │ │ 消息    │ │ 报销    │ │ 考勤    │ │ 文件  │ │
│  │ 部门    │ │ 审批    │ │ 通讯录  │ │ 预算    │ │ 打卡    │ │       │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └───┬───┘ │
│       │           │           │           │           │         │     │
│       └───────────┴───────────┴───────────┴───────────┴─────────┘     │
│                              │                                            │
│              ┌───────────────┴───────────────┐                         │
│              │         消息队列层             │                         │
│              │       RabbitMQ :5672          │                         │
│              │  Exchange: solidoa.exchange   │                         │
│              │  DLX: solidoa.dlx (死信)       │                         │
│              └───────────────────────────────┘                         │
├──────────────────────────────┼────────────────────────────────────────────┤
│                         数据层                                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │ MySQL 8 │ │ Redis 7 │ │  MinIO  │ │ Nacos   │ │RabbitMQ │           │
│  │ :3306   │ │ :6379   │ │ :9000   │ │ :8848   │ │ :5672   │           │
│  │ 核心DB  │ │ 缓存    │ │ 文件存储│ │ 注册中心│ │ 消息队列│           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 服务划分与职责

| 服务名 | 端口 | 职责 | 数据库 | 技术栈 |
|--------|------|------|--------|--------|
| **gateway** | 8080 | API网关、认证、路由、限流熔断 | - | Spring Cloud Gateway + Sentinel |
| **system** | 8081 | 用户、部门、角色、权限 | oa_system | Spring Boot + MyBatis-Plus |
| **workflow** | 8082 | Camunda流程引擎、审批管理 | oa_workflow | Spring Boot + Camunda 7.19 |
| **collaboration** | 8083 | 消息、日程、通讯录 | oa_collaboration | Spring Boot + WebSocket |
| **finance** | 8084 | 报销、预算、报表 | oa_finance | Spring Boot |
| **attendance** | 8085 | 考勤打卡、统计、钉钉同步 | oa_attendance | Spring Boot |
| **file** | 8086 | 文件上传、下载、预览 | oa_common | Spring Boot + MinIO |
| **dingtalk** | 8087 | 钉钉同步、免登、消息推送 | oa_common | Spring Boot + 钉钉SDK |

### 2.3 数据存储策略（混合模式）

#### 2.3.1 独立数据库（核心业务）

| 数据库 | 服务 | 说明 |
|--------|------|------|
| **oa_system** | system | 用户、部门、角色、权限数据 |
| **oa_workflow** | workflow | 流程定义、审批记录 |
| **oa_collaboration** | collaboration | 消息、日程、通讯录 |
| **oa_finance** | finance | 报销单、预算、报表 |
| **oa_attendance** | attendance | 考勤记录、统计 |

#### 2.3.2 共享数据库（轻量服务）

| 数据库 | 服务 | 说明 |
|--------|------|------|
| **oa_common** | file, dingtalk | 文件元数据、钉钉同步日志 |

### 2.4 服务间通信设计

#### 2.4.1 同步通信（OpenFeign + Sentinel熔断）

```
┌─────────────────────────────────────────────────────────┐
│                    同步HTTP调用 + 熔断降级                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Client (发起调用)                                        │
│       │                                                  │
│       ├── SystemClient ────▶ System Service (8081)      │
│       │    (Sentinel Fallback)  获取用户/部门信息        │
│       │                                                  │
│       ├── WorkflowClient ──▶ Workflow Service (8082)    │
│       │    (Sentinel Fallback)  提交/查询审批              │
│       │                                                  │
│       └── FinanceClient ──▶ Finance Service (8084)      │
│           (Sentinel Fallback)  获取报销/预算数据          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**OpenFeign + Sentinel配置：**

```java
@FeignClient(name = "system-service", path = "/api/v1/system",
             fallback = SystemClientFallback.class)
public interface SystemClient {
    @GetMapping("/user/{id}")
    UserDTO getUserById(@PathVariable Long id);

    @GetMapping("/dept/tree")
    List<DeptTreeVO> getDeptTree();
}

// Fallback降级处理
@Component
public class SystemClientFallback implements SystemClient {
    @Override
    public UserDTO getUserById(Long id) {
        return null; // 返回降级数据或从缓存获取
    }
    @Override
    public List<DeptTreeVO> getDeptTree() {
        return Collections.emptyList();
    }
}
```

**Sentinel熔断规则：**

```java
@Configuration
public class SentinelConfig {
    @Bean
    public SentinelFallbackFactory sentinelFallbackFactory() {
        return new SentinelFallbackFactory();
    }
}

// 熔断规则配置
@Slf4j
public class SentinelFallbackFactory implements FallbackFactory<Object> {
    @Override
    public Object create(Throwable cause) {
        log.error("服务调用失败", cause);
        // 返回降级响应
        return Result.fail("服务暂时不可用，请稍后重试");
    }
}
```

#### 2.4.2 异步通信（RabbitMQ + 死信队列）

```
┌─────────────────────────────────────────────────────────┐
│                    异步消息 + 死信队列                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Exchange: solidoa.exchange (Topic)                      │
│       │                                                 │
│       ├── queue.approval.notify ──▶ 审批通知              │
│       │    │                                             │
│       │    └── [失败3次] ──▶ solidoa.dlx ──▶ dead.queue  │
│       │                                                 │
│       ├── queue.message.push ────▶ 消息推送              │
│       │    │                                             │
│       │    └── [失败3次] ──▶ solidoa.dlx ──▶ dead.queue  │
│       │                                                 │
│       ├── queue.dingtalk.sync ───▶ 钉钉同步              │
│       └── queue.attendance.sync ──▶ 考勤同步              │
│                                                         │
│  Dead Letter Exchange: solidoa.dlx                      │
│       └── dead.queue ──▶ 死信队列(人工处理)              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**RabbitMQ配置（含死信队列）：**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: solidoa
    password: solidoa123
    virtual-host: /solidoa
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 3000
          max-attempts: 3
          multiplier: 2

# 交换机和队列配置
rabbitmq:
  exchanges:
    - name: solidoa.exchange
      type: topic
      durable: true
    - name: solidoa.dlx
      type: direct
      durable: true

  queues:
    approval-notify:
      name: queue.approval.notify
      durable: true
      arguments:
        x-dead-letter-exchange: solidoa.dlx
        x-dead-letter-routing-key: approval.notify.dead
        x-message-ttl: 86400000  # 24小时
    message-push:
      name: queue.message.push
      durable: true
      arguments:
        x-dead-letter-exchange: solidoa.dlx
        x-dead-letter-routing-key: message.push.dead
    dingtalk-sync:
      name: queue.dingtalk.sync
      durable: true
      arguments:
        x-dead-letter-exchange: solidoa.dlx
        x-dead-letter-routing-key: dingtalk.sync.dead

  bindings:
    - queue: approval-notify
      exchange: solidoa.exchange
      routingKey: approval.notify.*
    - queue: message-push
      exchange: solidoa.exchange
      routingKey: message.push.*
    - queue: dingtalk-sync
      exchange: solidoa.exchange
      routingKey: dingtalk.sync.*
```

### 2.5 技术选型汇总

#### 后端技术栈

| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| **微服务框架** | Spring Boot | 3.2.x | 应用框架 |
| **微服务治理** | Spring Cloud Alibaba | 2023.x | 服务治理 |
| **熔断限流** | Sentinel | 1.8.x | 熔断降级、限流 |
| **服务注册** | Nacos | 2.2.x | 注册+配置中心 |
| **网关** | Spring Cloud Gateway | 4.x | API网关 |
| **ORM** | MyBatis-Plus | 3.5.x | 数据访问 |
| **工作流** | Camunda 7.x + 自研状态机 | 7.19 | 复杂流程+加签/转交 |
| **安全** | Spring Security + JWT | - | 认证授权 |
| **缓存** | Redis | 7.x | 分布式缓存/会话 |
| **消息队列** | RabbitMQ | 3.x | 异步通知+死信队列 |
| **定时任务** | ShedLock | 5.x | 分布式定时任务锁 |
| **数据库** | MySQL | 8.0 | 主数据库 |
| **文件存储** | MinIO | 最新 | 对象存储 |
| **工具库** | Lombok + Hutool | - | 工具库 |

#### 前端技术栈

| 端 | 技术选型 | 版本 | 说明 |
|----|----------|------|------|
| **Web管理端** | Vue3 + Vite | 3.x / 5.x | 前端框架 |
| **UI框架** | Element Plus | 2.x | UI组件库 |
| **状态管理** | Pinia | 2.x | 状态管理 |
| **HTTP** | Axios | 1.x | HTTP客户端 |
| **路由** | Vue Router | 4.x | 前端路由 |
| **移动端** | UniApp | 3.x | 跨平台框架 |

---

## 3. 核心模块设计

### 3.1 系统管理模块（system-service）

#### 3.1.1 模块职责

- 用户管理：CRUD、钉钉同步、批量导入、密码重置
- 部门管理：组织架构树CRUD
- 角色权限：RBAC模型、权限配置、用户赋权
- 基础数据：枚举字典、系统配置

#### 3.1.2 数据模型

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    mobile VARCHAR(20) COMMENT '手机号(AES加密)',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    dept_id BIGINT COMMENT '部门ID',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    dingtalk_userid VARCHAR(100) COMMENT '钉钉用户ID',
    dingtalk_unionid VARCHAR(100) COMMENT '钉钉UnionID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_dept_id (dept_id),
    INDEX idx_dingtalk_userid (dingtalk_userid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 部门表
CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    leader_id BIGINT COMMENT '部门负责人ID',
    sort INT DEFAULT 0 COMMENT '排序号',
    dingtalk_id VARCHAR(100) COMMENT '钉钉部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户角色表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) COMMENT '类型:menu,button,api',
    url VARCHAR(200) COMMENT '请求路径',
    method VARCHAR(10) COMMENT '请求方法:GET,POST,PUT,DELETE',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    sort INT DEFAULT 0 COMMENT '排序号',
    icon VARCHAR(50) COMMENT '图标',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色权限表
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 数据字典表
CREATE TABLE sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL COMMENT '字典类型',
    label VARCHAR(100) NOT NULL COMMENT '字典标签',
    value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.1.3 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | /api/v1/auth/login | 用户登录 |
| 登出 | POST | /api/v1/auth/logout | 用户登出 |
| 刷新Token | POST | /api/v1/auth/refresh | 刷新访问令牌 |
| 用户列表 | GET | /api/v1/system/users | 分页查询用户 |
| 新增用户 | POST | /api/v1/system/users | 创建用户 |
| 修改用户 | PUT | /api/v1/system/users/{id} | 修改用户 |
| 删除用户 | DELETE | /api/v1/system/users/{id} | 删除用户 |
| 重置密码 | PUT | /api/v1/system/users/{id}/password | 重置密码 |
| 部门树 | GET | /api/v1/system/depts/tree | 获取部门树 |
| 新增部门 | POST | /api/v1/system/depts | 创建部门 |
| 修改部门 | PUT | /api/v1/system/depts/{id} | 修改部门 |
| 删除部门 | DELETE | /api/v1/system/depts/{id} | 删除部门 |
| 角色列表 | GET | /api/v1/system/roles | 角色列表 |
| 配置权限 | PUT | /api/v1/system/roles/{id}/permissions | 配置角色权限 |
| 钉钉同步 | POST | /api/v1/system/sync/dingtalk | 同步钉钉用户 |

#### 3.1.4 缓存设计

| 缓存Key | 过期时间 | 说明 |
|--------|----------|------|
| `user:{id}` | 30分钟 | 用户信息 |
| `user:username:{username}` | 30分钟 | 用户名查询 |
| `dept:tree` | 2小时 | 部门树 |
| `dept:{id}` | 1小时 | 部门详情 |
| `permission:role:{id}` | 1小时 | 角色权限 |
| `dict:type:{type}` | 2小时 | 字典数据 |

### 3.2 审批流程模块（workflow-service）

#### 3.2.1 模块职责

- 工作流引擎：Camunda管理复杂审批流程（BPMN）
- 审批管理：请假、报销、通用审批
- 加签/转交：自研状态机处理（动态节点，非Camunda流程）
- 催办提醒：RabbitMQ消息通知

#### 3.2.2 Camunda + 自研状态机职责划分

```
┌─────────────────────────────────────────────────────────┐
│              Camunda与状态机职责划分                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Camunda负责（复杂BPMN流程）                              │
│  ├── 请假审批流程（多级审批、条件分支）                     │
│  ├── 费用报销流程（部门审批→财务审批）                     │
│  └── 通用审批模板（可配置的BPMN）                         │
│                                                         │
│  自研状态机负责（动态节点）                               │
│  ├── 加签：动态增加审批节点，不影响原流程                  │
│  ├── 转交：将审批权转移给其他人                           │
│  └── 催办：发送提醒，不改变流程状态                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**加签/转交流程：**

```
原审批流程：
  申请人 ──▶ 部门经理审批 ──▶ 二级审批 ──▶ 结束

加签场景（部门经理A需要总监B协助审批）：
  部门经理A ──▶ [加签] ──▶ 总监B（并行） ──▶ [合并] ──▶ 二级审批

转交场景（A将审批权转给C）：
  A ──▶ [转交] ──▶ C替代A继续审批
```

#### 3.2.3 Camunda流程设计

**请假审批流程：**

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  开始   │───▶│ 填写申请 │───▶│ 直属审批 │───▶│ 二级审批 │───▶│  结束   │
└─────────┘    └─────────┘    └────┬────┘    └────┬────┘    └─────────┘
                                  │              │
                                  ▼              ▼
                              [拒绝]        [拒绝]
                                  │              │
                                  ▼              ▼
                              ┌─────────┐    ┌─────────┐
                              │  驳回   │    │  驳回   │
                              └─────────┘    └─────────┘
```

#### 3.2.4 自研状态机设计

```java
// 审批状态枚举
public enum ApprovalStatus {
    PENDING,     // 待审批
    APPROVED,    // 已同意
    REJECTED,    // 已拒绝
    TRANSFERRED, // 已转交
    ADD_SIGN,    // 加签中
    CANCELLED,   // 已撤回
    COMPLETED    // 已完成
}

// 加签/转交记录
public class ApprovalActionRecord {
    private Long id;
    private String businessType;     // 业务类型
    private Long businessId;          // 业务ID
    private Long fromUserId;          // 原审批人
    private Long toUserId;            // 新审批人
    private String actionType;        // ADD_SIGN / TRANSFER
    private String reason;            // 原因
    private LocalDateTime createTime;
}

// 状态机流转规则
public class ApprovalStateMachine {
    // PENDING -> APPROVED (同意)
    // PENDING -> REJECTED (拒绝)
    // PENDING -> TRANSFERRED (转交后，原审批人状态)
    // PENDING -> ADD_SIGN (加签中)
    // PENDING -> CANCELLED (撤回)
    // APPROVED -> COMPLETED (流程结束)
}
```

#### 3.2.5 数据模型

```sql
-- 请假申请表
CREATE TABLE oa_leave (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_no VARCHAR(32) NOT NULL UNIQUE COMMENT '请假单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型:ANNUAL病假,SICK事假,PERSONAL调休,BUSINESS出差',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    hours DECIMAL(5,1) DEFAULT 0 COMMENT '请假小时数',
    reason TEXT COMMENT '请假事由',
    attachments VARCHAR(500) COMMENT '附件(逗号分隔)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销申请表
CREATE TABLE oa_expense (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expense_no VARCHAR(32) NOT NULL UNIQUE COMMENT '报销单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    expense_type VARCHAR(50) NOT NULL COMMENT '报销类型:TRAVEL差旅费,OFFICE办公费,ENTERTAINMENT业务招待费,OTHER其他',
    amount DECIMAL(10,2) NOT NULL COMMENT '报销金额',
    reason TEXT COMMENT '报销事由',
    attachments VARCHAR(500) COMMENT '附件(逗号分隔)',
    bank_name VARCHAR(50) COMMENT '开户行',
    bank_account VARCHAR(50) COMMENT '银行账号(AES加密)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    payment_time DATETIME COMMENT '付款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审批记录表（包含加签/转交）
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型:LEAVE,EXPENSE',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approve_type VARCHAR(20) NOT NULL COMMENT '审批类型:APPROVE同意,REJECT拒绝,TRANSFER转交,ADD_SIGN加签',
    comment TEXT COMMENT '审批意见',
    task_id VARCHAR(100) COMMENT 'Camunda任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id, create_time),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 转交记录表
CREATE TABLE oa_transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    from_approver_id BIGINT NOT NULL COMMENT '原审批人',
    to_approver_id BIGINT NOT NULL COMMENT '新审批人',
    reason VARCHAR(200) COMMENT '转交原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 催办记录表
CREATE TABLE oa_reminder_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL,
    business_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    reminder_count INT DEFAULT 0 COMMENT '催办次数',
    last_reminder_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.2.6 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 新建请假 | POST | /api/v1/workflow/leave | 创建请假申请 |
| 请假列表 | GET | /api/v1/workflow/leave | 分页查询请假 |
| 请假详情 | GET | /api/v1/workflow/leave/{id} | 请假详情 |
| 审批请假 | POST | /api/v1/workflow/leave/{id}/approve | 审批请假 |
| 撤回请假 | PUT | /api/v1/workflow/leave/{id}/cancel | 撤回请假 |
| 加签 | POST | /api/v1/workflow/leave/{id}/add-sign | 加签 |
| 转交 | POST | /api/v1/workflow/leave/{id}/transfer | 转交 |
| 催办 | POST | /api/v1/workflow/leave/{id}/remind | 催办 |
| 新建报销 | POST | /api/v1/workflow/expense | 创建报销申请 |
| 报销列表 | GET | /api/v1/workflow/expense | 分页查询报销 |
| 审批报销 | POST | /api/v1/workflow/expense/{id}/approve | 审批报销 |
| 新建用印 | POST | /api/v1/workflow/stamp | 创建用印申请 |
| 用印列表 | GET | /api/v1/workflow/stamp | 分页查询用印 |
| 用印详情 | GET | /api/v1/workflow/stamp/{id} | 用印详情 |
| 审批用印 | POST | /api/v1/workflow/stamp/{id}/approve | 审批用印 |
| 用印登记 | POST | /api/v1/workflow/stamp/{id}/record | 物理用印登记 |
| 用印统计 | GET | /api/v1/workflow/stamp/statistics | 用印统计 |
| 新建采购 | POST | /api/v1/workflow/purchase | 创建采购申请 |
| 采购列表 | GET | /api/v1/workflow/purchase | 分页查询采购 |
| 采购详情 | GET | /api/v1/workflow/purchase/{id} | 采购详情 |
| 审批采购 | POST | /api/v1/workflow/purchase/{id}/approve | 审批采购 |
| 更新进度 | PUT | /api/v1/workflow/purchase/{id}/progress | 更新交付状态 |
| 采购统计 | GET | /api/v1/workflow/purchase/statistics | 采购统计 |
| 待我审批 | GET | /api/v1/workflow/tasks/pending | 待审批列表 |
| 我已审批 | GET | /api/v1/workflow/tasks/approved | 已审批列表 |
| 我发起的 | GET | /api/v1/workflow/tasks/my-apply | 我发起的 |

### 3.2.7 用印申请模块

##### 3.2.7.1 模块职责

- 用印申请：公章、合同章、法人章、部门章的使用申请
- 用印审批：部门负责人→行政主管（或法务）的审批流程
- 用印记录：记录用印时间、领用人、份数等
- 用印统计：按部门、人员统计用印次数

##### 3.2.7.2 审批流程设计

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  开始   │───▶│ 填写申请 │───▶│ 部门审批 │───▶│ 行政审批 │───▶│  结束   │
└─────────┘    └─────────┘    └────┬────┘    └────┬────┘    └─────────┘
                                   │              │
                                   ▼              ▼
                               [拒绝]          [拒绝]
                                   │              │
                                   ▼              ▼
                               ┌─────────┐    ┌─────────┐
                               │  驳回   │    │  驳回   │
                               └─────────┘    └─────────┘

特殊说明：
- 公章、法人章：必须行政主管审批
- 合同章：需要行政主管或法务审批
- 部门章：只需部门负责人审批
```

##### 3.2.7.3 用印类型说明

| 用印类型 | 类型码 | 说明 | 审批流程 |
|----------|--------|------|----------|
| 公章 | PUBLIC | 公司公章 | 申请人→部门负责人→行政主管 |
| 合同章 | CONTRACT | 合同专用章 | 申请人→部门负责人→行政主管/法务 |
| 法人章 | LEGAL | 法人私章 | 申请人→部门负责人→行政主管 |
| 部门章 | DEPT | 部门章 | 申请人→部门负责人 |

##### 3.2.7.4 数据模型

```sql
-- 用印申请表
CREATE TABLE oa_stamp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stamp_no VARCHAR(32) NOT NULL UNIQUE COMMENT '用印单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    stamp_type VARCHAR(20) NOT NULL COMMENT '用印类型:PUBLIC,CONTRACT,LEGAL,DEPT',
    document_name VARCHAR(200) NOT NULL COMMENT '文件名称',
    document_count INT DEFAULT 1 COMMENT '用印份数',
    usage VARCHAR(500) COMMENT '用印用途',
    attachments VARCHAR(500) COMMENT '附件(逗号分隔)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING审批中,APPROVED已同意,REJECTED已拒绝,COMPLETED已完成,CANCELLED已撤回',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    stamp_time DATETIME COMMENT '用印时间',
    received_by VARCHAR(50) COMMENT '领用人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_stamp_type (stamp_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用印记录表（物理用印）
CREATE TABLE oa_stamp_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stamp_id BIGINT NOT NULL COMMENT '用印申请ID',
    stamp_time DATETIME NOT NULL COMMENT '用印时间',
    received_by VARCHAR(50) NOT NULL COMMENT '领用人',
    received_mobile VARCHAR(20) COMMENT '领用人手机',
    actual_count INT NOT NULL COMMENT '实际用印份数',
    operator_id BIGINT COMMENT '办理人ID',
    operator_name VARCHAR(50) COMMENT '办理人姓名',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stamp_id (stamp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

##### 3.2.7.5 用印类型枚举

```java
/**
 * 用印类型枚举
 */
public enum StampType {
    PUBLIC("PUBLIC", "公章"),
    CONTRACT("CONTRACT", "合同章"),
    LEGAL("LEGAL", "法人章"),
    DEPT("DEPT", "部门章");

    private final String code;
    private final String desc;

    StampType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static StampType fromCode(String code) {
        return Arrays.stream(values())
            .filter(t -> t.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("无效的用印类型: " + code));
    }

    /** 是否需要行政主管审批 */
    public boolean needAdminApproval() {
        return this == PUBLIC || this == CONTRACT || this == LEGAL;
    }

    /** 是否需要法务审批 */
    public boolean needLegalApproval() {
        return this == CONTRACT;
    }
}
```

##### 3.2.7.6 Camunda流程设计

**用印申请BPMN流程定义：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  id="Definitions_Stamp" targetNamespace="http://bpmn.io/schema/bpmn">

  <bpmn:process id="Process_Stamp" name="用印申请流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_Stamp" name="开始"/>

    <!-- 填写申请 -->
    <bpmn:userTask id="Activity_Apply" name="填写申请"/>

    <!-- 用印类型判断网关 -->
    <bpmn:exclusiveGateway id="Gateway_StampType" name="用印类型判断"/>

    <!-- 审批节点 -->
    <bpmn:userTask id="Activity_DeptApproval" name="部门负责人审批"/>
    <bpmn:userTask id="Activity_AdminApproval" name="行政主管审批"/>
    <bpmn:userTask id="Activity_LegalApproval" name="法务审批"/>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_Approved" name="审批通过"/>
    <bpmn:endEvent id="EndEvent_Rejected" name="审批拒绝"/>

    <!-- 流程连接 -->
    <bpmn:sequenceFlow id="Flow_Start" sourceRef="StartEvent_Stamp" targetRef="Activity_Apply"/>
    <bpmn:sequenceFlow id="Flow_Apply" sourceRef="Activity_Apply" targetRef="Gateway_StampType"/>

    <!-- 部门章：仅部门审批 -->
    <bpmn:sequenceFlow id="Flow_DeptOnly" sourceRef="Gateway_StampType" targetRef="Activity_DeptApproval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType == 'DEPT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <!-- 公章/法人章：部门+行政 -->
    <bpmn:sequenceFlow id="Flow_NeedAdmin" sourceRef="Gateway_StampType" targetRef="Activity_DeptApproval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType in ['PUBLIC', 'LEGAL']}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <!-- 合同章：部门+法务 -->
    <bpmn:sequenceFlow id="Flow_NeedLegal" sourceRef="Gateway_StampType" targetRef="Activity_DeptApproval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType == 'CONTRACT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <!-- 部门审批后分流 -->
    <bpmn:sequenceFlow id="Flow_DeptToAdmin" sourceRef="Activity_DeptApproval" targetRef="Activity_AdminApproval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType in ['PUBLIC', 'LEGAL']}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_DeptToLegal" sourceRef="Activity_DeptApproval" targetRef="Activity_LegalApproval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType == 'CONTRACT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_DeptOnlyEnd" sourceRef="Activity_DeptApproval" targetRef="EndEvent_Approved">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${stampType == 'DEPT'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <!-- 行政/法务审批通过 -->
    <bpmn:sequenceFlow id="Flow_AdminEnd" sourceRef="Activity_AdminApproval" targetRef="EndEvent_Approved"/>
    <bpmn:sequenceFlow id="Flow_LegalEnd" sourceRef="Activity_LegalApproval" targetRef="EndEvent_Approved"/>

    <!-- 拒绝路径 -->
    <bpmn:sequenceFlow id="Flow_DeptReject" sourceRef="Activity_DeptApproval" targetRef="EndEvent_Rejected"/>
    <bpmn:sequenceFlow id="Flow_AdminReject" sourceRef="Activity_AdminApproval" targetRef="EndEvent_Rejected"/>
    <bpmn:sequenceFlow id="Flow_LegalReject" sourceRef="Activity_LegalApproval" targetRef="EndEvent_Rejected"/>
  </bpmn:process>
</bpmn:definitions>
```

##### 3.2.7.7 Controller接口定义

```java
/**
 * 用印申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/stamp")
@RequiredArgsConstructor
@Tag(name = "用印申请", description = "用印申请相关接口")
public class StampController {

    private final StampService stampService;

    @PostMapping
    @Operation(summary = "新建用印申请")
    public Result<Long> createStamp(@Valid @RequestBody StampForm form,
                                    @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(stampService.createStamp(form, userId));
    }

    @GetMapping
    @Operation(summary = "用印列表")
    public Result<PageResult<StampVO>> listStamp(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stampType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(stampService.listStamp(pageNum, pageSize, status, stampType, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用印详情")
    public Result<StampVO> getStampById(@PathVariable Long id) {
        return Result.success(stampService.getStampById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批用印")
    public Result<Void> approveStamp(@PathVariable Long id,
                                     @Valid @RequestBody ApproveForm form,
                                     @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        stampService.approveStamp(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回用印申请")
    public Result<Void> cancelStamp(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        stampService.cancelStamp(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/record")
    @Operation(summary = "物理用印登记")
    public Result<Void> recordStamp(@PathVariable Long id,
                                     @Valid @RequestBody StampRecordForm form,
                                     @RequestHeader("Authorization") String token) {
        Long operatorId = getUserIdFromToken(token);
        stampService.recordStamp(id, form, operatorId);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "用印统计")
    public Result<StampStatisticsVO> getStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long deptId) {
        return Result.success(stampService.getStatistics(startDate, endDate, deptId));
    }

    @GetMapping("/types")
    @Operation(summary = "用印类型列表")
    public Result<List<EnumVO>> getStampTypes() {
        return Result.success(stampService.getStampTypes());
    }
}
```

##### 3.2.7.8 Service核心逻辑

```java
/**
 * 用印申请Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StampService {

    private final StampMapper stampMapper;
    private final StampRecordMapper stampRecordMapper;
    private final CamundaClient camundaClient;
    private final MessageClient messageClient;

    /**
     * 创建用印申请
     */
    @Transactional
    public Long createStamp(StampForm form, Long userId) {
        // 1. 验证用户和部门
        UserDTO user = getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 生成用印单号 YS + 年月日 + 序号
        String stampNo = generateStampNo();

        // 3. 确定审批人
        Long deptLeaderId = getDeptLeaderId(user.getDeptId());

        // 4. 创建申请记录
        Stamp stamp = new Stamp();
        stamp.setStampNo(stampNo);
        stamp.setUserId(userId);
        stamp.setDeptId(user.getDeptId());
        stamp.setStampType(form.getStampType());
        stamp.setDocumentName(form.getDocumentName());
        stamp.setDocumentCount(form.getDocumentCount());
        stamp.setUsage(form.getUsage());
        stamp.setAttachments(String.join(",", form.getAttachments()));
        stamp.setStatus(ApprovalStatus.PENDING.getCode());
        stamp.setCurrentApproverId(deptLeaderId);
        stampMapper.insert(stamp);

        // 5. 启动Camunda流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("stampType", form.getStampType());
        variables.put("userId", userId);
        variables.put("deptId", user.getDeptId());
        variables.put("stampId", stamp.getId());
        variables.put("needLegalApproval", StampType.fromCode(form.getStampType()).needLegalApproval());

        String processInstanceId = camundaClient.startProcess("Process_Stamp", variables);
        stamp.setProcessInstanceId(processInstanceId);
        stampMapper.updateById(stamp);

        // 6. 发送消息通知审批人
        messageClient.sendApprovalNotify(deptLeaderId, "STAMP", stamp.getId(), "用印申请待审批");

        log.info("创建用印申请成功: stampNo={}, userId={}", stampNo, userId);
        return stamp.getId();
    }

    /**
     * 审批用印申请
     */
    @Transactional
    public void approveStamp(Long stampId, ApproveForm form, Long approverId) {
        Stamp stamp = stampMapper.selectById(stampId);
        if (stamp == null) {
            throw new BusinessException("用印申请不存在");
        }

        // 验证审批人
        if (!stamp.getCurrentApproverId().equals(approverId)) {
            throw new BusinessException("您不是当前审批人");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(stamp.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        // 保存审批记录
        saveApprovalRecord(stampId, "STAMP", approverId, form.getApproveType(), form.getComment());

        if ("APPROVE".equals(form.getApproveType())) {
            // 同意：完成任务，推进Camunda流程
            camundaClient.completeTask(stamp.getProcessInstanceId(), approverId);

            // 更新申请状态
            stamp.setStatus(ApprovalStatus.APPROVED.getCode());
            stamp.setUpdateTime(LocalDateTime.now());
            stampMapper.updateById(stamp);

            // 根据用印类型判断是否需要继续审批
            Long nextApproverId = getNextApprover(stamp);
            if (nextApproverId != null) {
                stamp.setCurrentApproverId(nextApproverId);
                stamp.setStatus(ApprovalStatus.PENDING.getCode());
                stampMapper.updateById(stamp);
                messageClient.sendApprovalNotify(nextApproverId, "STAMP", stampId, "用印申请待审批");
            }

            // 通知申请人
            messageClient.sendMessage(stamp.getUserId(), "用印申请已通过", "您提交的用印申请已审批通过");

        } else if ("REJECT".equals(form.getApproveType())) {
            // 拒绝
            stamp.setStatus(ApprovalStatus.REJECTED.getCode());
            stamp.setUpdateTime(LocalDateTime.now());
            stampMapper.updateById(stamp);

            // 通知申请人
            messageClient.sendMessage(stamp.getUserId(), "用印申请被拒绝",
                "您提交的用印申请被拒绝，原因：" + form.getComment());
        }

        log.info("审批用印申请: stampId={}, approverId={}, type={}", stampId, approverId, form.getApproveType());
    }

    /**
     * 物理用印登记
     */
    @Transactional
    public void recordStamp(Long stampId, StampRecordForm form, Long operatorId) {
        Stamp stamp = stampMapper.selectById(stampId);
        if (stamp == null) {
            throw new BusinessException("用印申请不存在");
        }

        if (!ApprovalStatus.APPROVED.getCode().equals(stamp.getStatus())) {
            throw new BusinessException("仅已审批通过的申请可以登记用印");
        }

        // 创建用印记录
        StampRecord record = new StampRecord();
        record.setStampId(stampId);
        record.setStampTime(form.getStampTime());
        record.setReceivedBy(form.getReceivedBy());
        record.setReceivedMobile(form.getReceivedMobile());
        record.setActualCount(form.getActualCount());
        record.setOperatorId(operatorId);
        record.setOperatorName(getUserById(operatorId).getRealName());
        record.setRemark(form.getRemark());
        stampRecordMapper.insert(record);

        // 更新申请状态
        stamp.setStampTime(form.getStampTime());
        stamp.setReceivedBy(form.getReceivedBy());
        stamp.setStatus(ApprovalStatus.COMPLETED.getCode());
        stampMapper.updateById(stamp);

        log.info("登记物理用印: stampId={}, receivedBy={}", stampId, form.getReceivedBy());
    }

    /**
     * 生成用印单号
     */
    private String generateStampNo() {
        String prefix = "YS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxNo = stampMapper.selectMaxStampNo(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        int seq = Integer.parseInt(maxNo.substring(maxNo.length() - 4)) + 1;
        return prefix + String.format("%04d", seq);
    }
}
```

---

### 3.2.8 采购申请模块

##### 3.2.8.1 模块职责

- 采购申请：办公用品、IT设备、家具、软件/服务的采购申请
- 采购审批：根据金额阈值决定审批流程
- 供应商管理：可选填供应商信息
- 采购进度：跟踪采购到货状态

##### 3.2.8.2 审批流程设计

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  开始   │───▶│ 填写申请 │───▶│ 部门审批 │───▶│ 采购审批 │───▶│  结束   │
└─────────┘    └─────────┘    └────┬────┘    └────┬────┘    └─────────┘
                                   │              │
                                   ▼              ▼
                               [拒绝]          [拒绝]
                                   │              │
                                   ▼              ▼
                               ┌─────────┐    ┌─────────┐
                               │  驳回   │    │  驳回   │
                               └─────────┘    └─────────┘

阈值规则：
- 金额 < 5000元：申请人→部门负责人（直接通过）
- 金额 >= 5000元：申请人→部门负责人→采购部门
- 金额 >= 20000元：申请人→部门负责人→采购部门→财务审批
```

##### 3.2.8.3 采购类型说明

| 采购类型 | 类型码 | 说明 | 预算归属 |
|----------|--------|------|----------|
| 办公用品 | OFFICE | 笔、本、打印耗材等 | 行政预算 |
| IT设备 | IT | 电脑、显示器、键盘等 | IT预算 |
| 家具 | FURNITURE | 桌椅、柜子等 | 行政预算 |
| 软件/服务 | SOFTWARE | 软件授权、云服务等 | IT预算 |
| 其他 | OTHER | 其他采购 | 视情况 |

##### 3.2.8.4 数据模型

```sql
-- 采购申请表
CREATE TABLE oa_purchase (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_no VARCHAR(32) NOT NULL UNIQUE COMMENT '采购单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    purchase_type VARCHAR(20) NOT NULL COMMENT '采购类型:OFFICE,IT,FURNITURE,SOFTWARE,OTHER',
    item_name VARCHAR(200) NOT NULL COMMENT '物品名称',
    quantity INT DEFAULT 1 COMMENT '采购数量',
    unit VARCHAR(20) COMMENT '单位',
    budget_amount DECIMAL(10,2) COMMENT '预算金额',
    supplier_name VARCHAR(100) COMMENT '供应商名称',
    supplier_contact VARCHAR(50) COMMENT '供应商联系人',
    supplier_phone VARCHAR(20) COMMENT '供应商电话',
    reason TEXT COMMENT '采购原因',
    attachments VARCHAR(500) COMMENT '附件(逗号分隔)',
    delivery_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '交付状态:PENDING待采购,PURCHASING采购中,DELIVERED已到货,COMPLETED已完成',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态:PENDING审批中,APPROVED已同意,REJECTED已拒绝,COMPLETED已完成,CANCELLED已撤回',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    expected_delivery_date DATE COMMENT '期望交付日期',
    actual_delivery_date DATE COMMENT '实际交付日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_purchase_type (purchase_type),
    INDEX idx_delivery_status (delivery_status),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采购明细表（复杂采购）
CREATE TABLE oa_purchase_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL COMMENT '采购单ID',
    item_name VARCHAR(200) NOT NULL COMMENT '物品名称',
    spec VARCHAR(200) COMMENT '规格型号',
    quantity INT DEFAULT 1 COMMENT '数量',
    unit VARCHAR(20) COMMENT '单位',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_price DECIMAL(10,2) COMMENT '总价',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchase_id (purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采购进度记录
CREATE TABLE oa_purchase_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL COMMENT '采购单ID',
    progress_type VARCHAR(20) NOT NULL COMMENT '进度类型:ORDERED已下单,SHIPPING发货中,DELIVERED已到货,REJECTED拒收',
    progress_desc VARCHAR(200) COMMENT '进度描述',
    progress_time DATETIME NOT NULL COMMENT '进度时间',
    operator_id BIGINT COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchase_id (purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

##### 3.2.8.5 采购类型枚举和阈值常量

```java
/**
 * 采购类型枚举
 */
public enum PurchaseType {
    OFFICE("OFFICE", "办公用品", "行政预算"),
    IT("IT", "IT设备", "IT预算"),
    FURNITURE("FURNITURE", "家具", "行政预算"),
    SOFTWARE("SOFTWARE", "软件/服务", "IT预算"),
    OTHER("OTHER", "其他", "其他");

    private final String code;
    private final String desc;
    private final String budgetCategory;

    PurchaseType(String code, String desc, String budgetCategory) {
        this.code = code;
        this.desc = desc;
        this.budgetCategory = budgetCategory;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public String getBudgetCategory() { return budgetCategory; }

    public static PurchaseType fromCode(String code) {
        return Arrays.stream(values())
            .filter(t -> t.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("无效的采购类型: " + code));
    }
}

/**
 * 采购审批阈值常量
 */
public class PurchaseThreshold {
    // 需要采购部门审批的金额阈值
    public static final BigDecimal PURCHASE_DEPT_THRESHOLD = new BigDecimal("5000");
    // 需要财务审批的金额阈值
    public static final BigDecimal FINANCE_APPROVAL_THRESHOLD = new BigDecimal("20000");

    public static boolean needPurchaseApproval(BigDecimal amount) {
        return amount != null && amount.compareTo(PURCHASE_DEPT_THRESHOLD) >= 0;
    }

    public static boolean needFinanceApproval(BigDecimal amount) {
        return amount != null && amount.compareTo(FINANCE_APPROVAL_THRESHOLD) >= 0;
    }

    public static int getApprovalNodeCount(BigDecimal amount) {
        if (needFinanceApproval(amount)) return 3; // 部门+采购+财务
        if (needPurchaseApproval(amount)) return 2; // 部门+采购
        return 1; // 仅部门
    }
}

/**
 * 交付状态枚举
 */
public enum DeliveryStatus {
    PENDING("PENDING", "待采购"),
    PURCHASING("PURCHASING", "采购中"),
    DELIVERED("DELIVERED", "已到货"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;

    DeliveryStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
```

##### 3.2.8.6 Camunda流程设计

**采购申请BPMN流程定义：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  id="Definitions_Purchase" targetNamespace="http://bpmn.io/schema/bpmn">

  <bpmn:process id="Process_Purchase" name="采购申请流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_Purchase" name="开始"/>

    <!-- 填写申请 -->
    <bpmn:userTask id="Activity_Purchase_Apply" name="填写采购申请"/>

    <!-- 金额判断网关 -->
    <bpmn:exclusiveGateway id="Gateway_Amount" name="金额判断"/>

    <!-- 审批节点 -->
    <bpmn:userTask id="Activity_Dept_Approval" name="部门负责人审批"/>
    <bpmn:userTask id="Activity_Purchase_Dept" name="采购部门审批"/>
    <bpmn:userTask id="Activity_Finance_Approval" name="财务审批"/>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_Approved" name="审批通过"/>
    <bpmn:endEvent id="EndEvent_Rejected" name="审批拒绝"/>

    <!-- 流程连接 -->
    <bpmn:sequenceFlow id="Flow_Start" sourceRef="StartEvent_Purchase" targetRef="Activity_Purchase_Apply"/>
    <bpmn:sequenceFlow id="Flow_Apply" sourceRef="Activity_Purchase_Apply" targetRef="Gateway_Amount"/>

    <!-- 金额 < 5000: 仅部门审批 -->
    <bpmn:sequenceFlow id="Flow_Low_Amount" sourceRef="Gateway_Amount" targetRef="Activity_Dept_Approval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &lt; 5000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Low_End" sourceRef="Activity_Dept_Approval" targetRef="EndEvent_Approved">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &lt; 5000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <!-- 5000 <= 金额 < 20000: 部门 + 采购 -->
    <bpmn:sequenceFlow id="Flow_Medium_Amount" sourceRef="Gateway_Amount" targetRef="Activity_Dept_Approval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &gt;= 5000 &amp;&amp; budgetAmount &lt; 20000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_To_Purchase_Dept" sourceRef="Activity_Dept_Approval" targetRef="Activity_Purchase_Dept">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &gt;= 5000 &amp;&amp; budgetAmount &lt; 20000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_Purchase_Dept_End" sourceRef="Activity_Purchase_Dept" targetRef="EndEvent_Approved"/>

    <!-- 金额 >= 20000: 部门 + 采购 + 财务 -->
    <bpmn:sequenceFlow id="Flow_High_Amount" sourceRef="Gateway_Amount" targetRef="Activity_Dept_Approval">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &gt;= 20000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_To_Finance" sourceRef="Activity_Dept_Approval" targetRef="Activity_Purchase_Dept">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${budgetAmount &gt;= 20000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_To_Finance_Dept" sourceRef="Activity_Purchase_Dept" targetRef="Activity_Finance_Approval"/>
    <bpmn:sequenceFlow id="Flow_Finance_End" sourceRef="Activity_Finance_Approval" targetRef="EndEvent_Approved"/>

    <!-- 拒绝路径 -->
    <bpmn:sequenceFlow id="Flow_Dept_Reject" sourceRef="Activity_Dept_Approval" targetRef="EndEvent_Rejected"/>
    <bpmn:sequenceFlow id="Flow_Purchase_Reject" sourceRef="Activity_Purchase_Dept" targetRef="EndEvent_Rejected"/>
    <bpmn:sequenceFlow id="Flow_Finance_Reject" sourceRef="Activity_Finance_Approval" targetRef="EndEvent_Rejected"/>
  </bpmn:process>
</bpmn:definitions>
```

##### 3.2.8.7 Controller接口定义

```java
/**
 * 采购申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/purchase")
@RequiredArgsConstructor
@Tag(name = "采购申请", description = "采购申请相关接口")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    @Operation(summary = "新建采购申请")
    public Result<Long> createPurchase(@Valid @RequestBody PurchaseForm form,
                                       @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(purchaseService.createPurchase(form, userId));
    }

    @GetMapping
    @Operation(summary = "采购列表")
    public Result<PageResult<PurchaseVO>> listPurchase(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String purchaseType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(purchaseService.listPurchase(pageNum, pageSize, status, deliveryStatus, purchaseType, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "采购详情")
    public Result<PurchaseVO> getPurchaseById(@PathVariable Long id) {
        return Result.success(purchaseService.getPurchaseById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批采购申请")
    public Result<Void> approvePurchase(@PathVariable Long id,
                                        @Valid @RequestBody ApproveForm form,
                                        @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        purchaseService.approvePurchase(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回采购申请")
    public Result<Void> cancelPurchase(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        purchaseService.cancelPurchase(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "更新采购进度")
    public Result<Void> updateProgress(@PathVariable Long id,
                                       @Valid @RequestBody PurchaseProgressForm form,
                                       @RequestHeader("Authorization") String token) {
        Long operatorId = getUserIdFromToken(token);
        purchaseService.updateProgress(id, form, operatorId);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "采购统计")
    public Result<PurchaseStatisticsVO> getStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String purchaseType) {
        return Result.success(purchaseService.getStatistics(startDate, endDate, deptId, purchaseType));
    }

    @GetMapping("/types")
    @Operation(summary = "采购类型列表")
    public Result<List<EnumVO>> getPurchaseTypes() {
        return Result.success(purchaseService.getPurchaseTypes());
    }
}
```

##### 3.2.8.8 Service核心逻辑

```java
/**
 * 采购申请Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final PurchaseItemMapper purchaseItemMapper;
    private final PurchaseProgressMapper progressMapper;
    private final CamundaClient camundaClient;
    private final MessageClient messageClient;

    /**
     * 创建采购申请
     */
    @Transactional
    public Long createPurchase(PurchaseForm form, Long userId) {
        // 1. 验证用户
        UserDTO user = getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 生成采购单号 CG + 年月日 + 序号
        String purchaseNo = generatePurchaseNo();

        // 3. 验证预算（如果设置了预算）
        BigDecimal budgetAmount = form.getBudgetAmount();
        if (budgetAmount != null && PurchaseThreshold.needFinanceApproval(budgetAmount)) {
            checkBudgetAvailable(user.getDeptId(), budgetAmount);
        }

        // 4. 确定审批人
        Long deptLeaderId = getDeptLeaderId(user.getDeptId());
        Long purchaseDeptLeaderId = getPurchaseDeptLeaderId();

        // 5. 创建采购申请
        Purchase purchase = new Purchase();
        purchase.setPurchaseNo(purchaseNo);
        purchase.setUserId(userId);
        purchase.setDeptId(user.getDeptId());
        purchase.setPurchaseType(form.getPurchaseType());
        purchase.setItemName(form.getItemName());
        purchase.setQuantity(form.getQuantity());
        purchase.setUnit(form.getUnit());
        purchase.setBudgetAmount(budgetAmount);
        purchase.setSupplierName(form.getSupplierName());
        purchase.setSupplierContact(form.getSupplierContact());
        purchase.setSupplierPhone(form.getSupplierPhone());
        purchase.setReason(form.getReason());
        purchase.setAttachments(form.getAttachments() != null ? String.join(",", form.getAttachments()) : null);
        purchase.setExpectedDeliveryDate(form.getExpectedDeliveryDate());
        purchase.setStatus(ApprovalStatus.PENDING.getCode());
        purchase.setDeliveryStatus(DeliveryStatus.PENDING.getCode());
        purchase.setCurrentApproverId(deptLeaderId);
        purchaseMapper.insert(purchase);

        // 6. 保存采购明细（如果有）
        if (form.getItems() != null && !form.getItems().isEmpty()) {
            for (PurchaseItemForm itemForm : form.getItems()) {
                PurchaseItem item = new PurchaseItem();
                item.setPurchaseId(purchase.getId());
                item.setItemName(itemForm.getItemName());
                item.setSpec(itemForm.getSpec());
                item.setQuantity(itemForm.getQuantity());
                item.setUnit(itemForm.getUnit());
                item.setUnitPrice(itemForm.getUnitPrice());
                item.setTotalPrice(itemForm.getUnitPrice().multiply(new BigDecimal(itemForm.getQuantity())));
                purchaseItemMapper.insert(item);
            }
        }

        // 7. 启动Camunda流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("budgetAmount", budgetAmount);
        variables.put("userId", userId);
        variables.put("deptId", user.getDeptId());
        variables.put("purchaseId", purchase.getId());
        variables.put("needPurchaseDept", PurchaseThreshold.needPurchaseApproval(budgetAmount));
        variables.put("needFinance", PurchaseThreshold.needFinanceApproval(budgetAmount));
        variables.put("purchaseDeptLeaderId", purchaseDeptLeaderId);

        String processInstanceId = camundaClient.startProcess("Process_Purchase", variables);
        purchase.setProcessInstanceId(processInstanceId);
        purchaseMapper.updateById(purchase);

        // 8. 发送消息通知审批人
        String approvalNode = PurchaseThreshold.needFinanceApproval(budgetAmount) ? "财务审批" :
                            PurchaseThreshold.needPurchaseApproval(budgetAmount) ? "采购部门审批" : "部门负责人审批";
        messageClient.sendApprovalNotify(deptLeaderId, "PURCHASE", purchase.getId(), "采购申请待" + approvalNode);

        log.info("创建采购申请成功: purchaseNo={}, userId={}, amount={}", purchaseNo, userId, budgetAmount);
        return purchase.getId();
    }

    /**
     * 审批采购申请
     */
    @Transactional
    public void approvePurchase(Long purchaseId, ApproveForm form, Long approverId) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new BusinessException("采购申请不存在");
        }

        if (!purchase.getCurrentApproverId().equals(approverId)) {
            throw new BusinessException("您不是当前审批人");
        }

        if (!ApprovalStatus.PENDING.getCode().equals(purchase.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        // 保存审批记录
        saveApprovalRecord(purchaseId, "PURCHASE", approverId, form.getApproveType(), form.getComment());

        if ("APPROVE".equals(form.getApproveType())) {
            camundaClient.completeTask(purchase.getProcessInstanceId(), approverId);

            // 确定下一个审批人
            Long nextApproverId = getNextPurchaseApprover(purchase, approverId);
            if (nextApproverId != null) {
                purchase.setCurrentApproverId(nextApproverId);
                purchaseMapper.updateById(purchase);
                String approvalNode = determineApprovalNode(purchase);
                messageClient.sendApprovalNotify(nextApproverId, "PURCHASE", purchaseId, "采购申请待" + approvalNode);
            } else {
                // 审批完成
                purchase.setStatus(ApprovalStatus.APPROVED.getCode());
                purchase.setDeliveryStatus(DeliveryStatus.PURCHASING.getCode());
                purchaseMapper.updateById(purchase);

                messageClient.sendMessage(purchase.getUserId(), "采购申请已通过",
                    "您提交的采购申请（" + purchase.getItemName() + "）已审批通过，请等待采购");
            }

        } else if ("REJECT".equals(form.getApproveType())) {
            purchase.setStatus(ApprovalStatus.REJECTED.getCode());
            purchaseMapper.updateById(purchase);

            // 恢复预算
            if (purchase.getBudgetAmount() != null) {
                releaseBudget(purchase.getDeptId(), purchase.getBudgetAmount());
            }

            messageClient.sendMessage(purchase.getUserId(), "采购申请被拒绝",
                "您提交的采购申请（" + purchase.getItemName() + "）被拒绝，原因：" + form.getComment());
        }

        log.info("审批采购申请: purchaseId={}, approverId={}, type={}", purchaseId, approverId, form.getApproveType());
    }

    /**
     * 更新采购进度
     */
    @Transactional
    public void updateProgress(Long purchaseId, PurchaseProgressForm form, Long operatorId) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new BusinessException("采购申请不存在");
        }

        if (!ApprovalStatus.APPROVED.getCode().equals(purchase.getStatus())) {
            throw new BusinessException("仅已审批通过的申请可以更新进度");
        }

        // 保存进度记录
        PurchaseProgress progress = new PurchaseProgress();
        progress.setPurchaseId(purchaseId);
        progress.setProgressType(form.getProgressType());
        progress.setProgressDesc(form.getProgressDesc());
        progress.setProgressTime(form.getProgressTime());
        progress.setOperatorId(operatorId);
        progressMapper.insert(progress);

        // 更新采购单交付状态
        purchase.setDeliveryStatus(form.getProgressType());
        if (DeliveryStatus.DELIVERED.getCode().equals(form.getProgressType())) {
            purchase.setActualDeliveryDate(LocalDate.now());
        } else if (DeliveryStatus.COMPLETED.getCode().equals(form.getProgressType())) {
            purchase.setDeliveryStatus(DeliveryStatus.COMPLETED.getCode());
            purchase.setActualDeliveryDate(LocalDate.now());
            messageClient.sendMessage(purchase.getUserId(), "采购已完成",
                "您申请的物品（" + purchase.getItemName() + "）已到货，请确认签收");
        }
        purchaseMapper.updateById(purchase);

        log.info("更新采购进度: purchaseId={}, type={}", purchaseId, form.getProgressType());
    }

    /**
     * 获取采购统计
     */
    public PurchaseStatisticsVO getStatistics(LocalDate startDate, LocalDate endDate, Long deptId, String purchaseType) {
        PurchaseStatisticsVO statistics = new PurchaseStatisticsVO();

        List<Purchase> purchases = purchaseMapper.selectStatistics(startDate, endDate, deptId, purchaseType);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int pendingCount = 0, approvedCount = 0, rejectedCount = 0;
        Map<String, BigDecimal> typeAmountMap = new HashMap<>();
        Map<String, Integer> typeCountMap = new HashMap<>();

        for (Purchase p : purchases) {
            totalAmount = totalAmount.add(p.getBudgetAmount() != null ? p.getBudgetAmount() : BigDecimal.ZERO);

            switch (p.getStatus()) {
                case "PENDING": pendingCount++; break;
                case "APPROVED":
                case "COMPLETED": approvedCount++; break;
                case "REJECTED": rejectedCount++; break;
            }

            String type = p.getPurchaseType();
            typeAmountMap.merge(type, p.getBudgetAmount() != null ? p.getBudgetAmount() : BigDecimal.ZERO, BigDecimal::add);
            typeCountMap.merge(type, 1, Integer::sum);
        }

        statistics.setTotalAmount(totalAmount);
        statistics.setPendingCount(pendingCount);
        statistics.setApprovedCount(approvedCount);
        statistics.setRejectedCount(rejectedCount);
        statistics.setTypeAmountMap(typeAmountMap);
        statistics.setTypeCountMap(typeCountMap);

        return statistics;
    }

    /**
     * 生成采购单号
     */
    private String generatePurchaseNo() {
        String prefix = "CG" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxNo = purchaseMapper.selectMaxPurchaseNo(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        int seq = Integer.parseInt(maxNo.substring(maxNo.length() - 4)) + 1;
        return prefix + String.format("%04d", seq);
    }

    /**
     * 获取下一个采购审批人
     */
    private Long getNextPurchaseApprover(Purchase purchase, Long currentApproverId) {
        BigDecimal amount = purchase.getBudgetAmount();
        Long deptId = purchase.getDeptId();

        if (PurchaseThreshold.needFinanceApproval(amount)) {
            Long deptLeaderId = getDeptLeaderId(deptId);
            Long purchaseDeptLeaderId = getPurchaseDeptLeaderId();
            Long financeLeaderId = getFinanceLeaderId();

            if (deptLeaderId.equals(currentApproverId)) {
                return purchaseDeptLeaderId;
            } else if (purchaseDeptLeaderId.equals(currentApproverId)) {
                return financeLeaderId;
            }
        } else if (PurchaseThreshold.needPurchaseApproval(amount)) {
            Long deptLeaderId = getDeptLeaderId(deptId);
            Long purchaseDeptLeaderId = getPurchaseDeptLeaderId();

            if (deptLeaderId.equals(currentApproverId)) {
                return purchaseDeptLeaderId;
            }
        }

        return null;
    }
}
```

### 3.3 协作办公模块（collaboration-service）

#### 3.3.1 模块职责

- 消息中心：系统通知、审批通知、实时推送（APP用WebSocket）
- 通讯录：员工列表、搜索、拨号
- 日程管理：创建日程、提醒、分享

#### 3.3.2 消息推送策略

```
┌─────────────────────────────────────────────────────────┐
│                  消息推送策略                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  APP端（支持WebSocket）                                  │
│       └── WebSocket实时推送                              │
│                                                         │
│  小程序端（不支持WebSocket）                              │
│       └── 钉钉工作通知（通过dingtalk-service）           │
│                                                         │
│  Web端                                                  │
│       └── WebSocket实时推送                              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 3.3.3 数据模型

```sql
-- 消息表
CREATE TABLE oa_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_no VARCHAR(32) NOT NULL UNIQUE COMMENT '消息编号',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    type VARCHAR(20) NOT NULL COMMENT '类型:SYSTEM系统,APPROVAL审批,ATTENDANCE考勤',
    priority TINYINT DEFAULT 0 COMMENT '优先级:0普通,1重要,2紧急',
    sender_id BIGINT COMMENT '发送人ID',
    sender_name VARCHAR(50) COMMENT '发送人姓名',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读:0未读,1已读',
    read_time DATETIME COMMENT '阅读时间',
    related_type VARCHAR(20) COMMENT '关联类型:LEAVE,EXPENSE',
    related_id BIGINT COMMENT '关联ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver (receiver_id, is_read, create_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通讯录表(冗余查询优化)
CREATE TABLE oa_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(100) COMMENT '部门名称',
    real_name VARCHAR(50) COMMENT '姓名',
    mobile VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    position VARCHAR(50) COMMENT '职位',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_dept_id (dept_id),
    INDEX idx_real_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日程表
CREATE TABLE oa_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '日程标题',
    content TEXT COMMENT '日程内容',
    location VARCHAR(200) COMMENT '地点',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    is_all_day TINYINT DEFAULT 0 COMMENT '是否全天',
    remind_before INT DEFAULT 15 COMMENT '提前提醒分钟数',
    remind_way VARCHAR(20) DEFAULT 'APP' COMMENT '提醒方式:APP,EMAIL,SMS',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '颜色标记',
    user_id BIGINT NOT NULL COMMENT '创建人',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态:NORMAL正常,CANCELLED已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日程分享表
CREATE TABLE oa_schedule_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL COMMENT '日程ID',
    share_user_id BIGINT NOT NULL COMMENT '分享人ID',
    receive_user_id BIGINT NOT NULL COMMENT '接收人ID',
    can_edit TINYINT DEFAULT 0 COMMENT '是否可编辑',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule_id (schedule_id),
    INDEX idx_receive_user (receive_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.3.4 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 消息列表 | GET | /api/v1/collab/messages | 消息列表 |
| 未读消息数 | GET | /api/v1/collab/messages/unread-count | 未读数 |
| 标记已读 | PUT | /api/v1/collab/messages/{id}/read | 标记已读 |
| 全部已读 | PUT | /api/v1/collab/messages/read-all | 全部已读 |
| 通讯录 | GET | /api/v1/collab/contacts | 通讯录列表 |
| 搜索员工 | GET | /api/v1/collab/contacts/search | 搜索员工 |
| 发送消息 | POST | /api/v1/collab/messages | 发送消息 |
| 日程列表 | GET | /api/v1/collab/schedules | 日程列表 |
| 新建日程 | POST | /api/v1/collab/schedules | 创建日程 |
| 修改日程 | PUT | /api/v1/collab/schedules/{id} | 修改日程 |
| 删除日程 | DELETE | /api/v1/collab/schedules/{id} | 删除日程 |
| 分享日程 | POST | /api/v1/collab/schedules/{id}/share | 分享日程 |

### 3.4 财务管理模块（finance-service）

#### 3.4.1 模块职责

- 报销管理：报销单审核、付款
- 预算管理：部门预算设置、使用跟踪
- 财务报表：报销明细、部门费用、趋势分析

#### 3.4.2 数据模型

```sql
-- 预算表
CREATE TABLE oa_budget (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    year INT NOT NULL COMMENT '预算年度',
    month INT NOT NULL COMMENT '预算月份',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '预算总额',
    used_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已使用金额',
    remaining_amount DECIMAL(12,2) GENERATED ALWAYS AS (total_amount - used_amount) STORED COMMENT '剩余金额',
    warning_threshold DECIMAL(5,2) DEFAULT 0.8 COMMENT '预警阈值(百分比)',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_month (dept_id, year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预算调整记录表
CREATE TABLE oa_budget_adjust (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    budget_id BIGINT NOT NULL COMMENT '预算ID',
    adjust_amount DECIMAL(12,2) NOT NULL COMMENT '调整金额',
    adjust_type VARCHAR(20) NOT NULL COMMENT '调整类型:ADD增,BACK减',
    reason VARCHAR(200) COMMENT '调整原因',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销明细表
CREATE TABLE oa_expense_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expense_id BIGINT NOT NULL COMMENT '报销单ID',
    item_date DATE NOT NULL COMMENT '费用日期',
    item_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    description VARCHAR(200) COMMENT '费用说明',
    attachment VARCHAR(500) COMMENT '票据URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 部门费用统计视图
CREATE VIEW v_dept_expense_summary AS
SELECT
    dept_id,
    DATE_FORMAT(create_time, '%Y-%m') AS month,
    SUM(amount) AS total_amount,
    COUNT(*) AS expense_count
FROM oa_expense
WHERE status = 'COMPLETED'
GROUP BY dept_id, DATE_FORMAT(create_time, '%Y-%m');
```

#### 3.4.3 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 报销单列表 | GET | /api/v1/finance/expense | 报销单列表 |
| 报销统计 | GET | /api/v1/finance/expense/statistics | 报销统计 |
| 预算列表 | GET | /api/v1/finance/budget | 预算列表 |
| 设置预算 | POST | /api/v1/finance/budget | 创建预算 |
| 预算预警 | GET | /api/v1/finance/budget/warnings | 预警列表 |
| 报销明细 | GET | /api/v1/finance/expense/{id}/details | 报销明细 |
| 部门费用 | GET | /api/v1/finance/report/dept-expense | 部门费用报表 |
| 导出报表 | GET | /api/v1/finance/report/export | 导出Excel |

### 3.5 考勤模块（attendance-service）

#### 3.5.1 模块职责

- 打卡记录：员工考勤打卡（APP/小程序）
- 请假记录：同步请假审批
- 考勤统计：月度统计、异常处理
- 钉钉同步：考勤数据同步

#### 3.5.2 数据模型

```sql
-- 打卡记录表
CREATE TABLE oa_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    check_date DATE NOT NULL COMMENT '打卡日期',
    check_type VARCHAR(20) NOT NULL COMMENT '打卡类型:SIGN_IN签到,SIGN_OUT签退',
    check_time DATETIME NOT NULL COMMENT '打卡时间',
    location VARCHAR(200) COMMENT '打卡地点',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    device_type VARCHAR(20) COMMENT '设备类型:APP,MINI_PROGRAM,DINGTALK',
    dingtalk_check_id VARCHAR(100) COMMENT '钉钉打卡ID',
    is_late TINYINT DEFAULT 0 COMMENT '是否迟到',
    is_early_leave TINYINT DEFAULT 0 COMMENT '是否早退',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, check_date),
    INDEX idx_dingtalk_id (dingtalk_check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 请假记录表(同步审批)
CREATE TABLE oa_leave_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_id BIGINT NOT NULL COMMENT '请假申请ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5,1) NOT NULL COMMENT '天数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT '流程实例ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 考勤统计表(月度汇总)
CREATE TABLE oa_attendance_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT COMMENT '部门ID',
    year_month VARCHAR(7) NOT NULL COMMENT '统计月份',
    work_days INT NOT NULL COMMENT '应到天数',
    actual_days INT NOT NULL COMMENT '实到天数',
    late_count INT DEFAULT 0 COMMENT '迟到次数',
    early_leave_count INT DEFAULT 0 COMMENT '早退次数',
    absent_days DECIMAL(5,1) DEFAULT 0 COMMENT '旷工天数',
    leave_days DECIMAL(5,1) DEFAULT 0 COMMENT '请假天数',
    business_days DECIMAL(5,1) DEFAULT 0 COMMENT '出差天数',
    overtime_hours DECIMAL(5,1) DEFAULT 0 COMMENT '加班小时数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, year_month),
    INDEX idx_dept_month (dept_id, year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 补卡申请表
CREATE TABLE oa_repair_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    repair_date DATE NOT NULL COMMENT '补卡日期',
    repair_type VARCHAR(20) NOT NULL COMMENT '补卡类型:SIGN_IN,SIGN_OUT',
    repair_time DATETIME NOT NULL COMMENT '补卡时间',
    reason VARCHAR(200) NOT NULL COMMENT '补卡原因',
    attachments VARCHAR(500) COMMENT '证明材料',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.5.3 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 打卡 | POST | /api/v1/attendance/check | 打卡 |
| 打卡记录 | GET | /api/v1/attendance/records | 打卡记录 |
| 月度统计 | GET | /api/v1/attendance/summary | 月度统计 |
| 考勤异常 | GET | /api/v1/attendance/exceptions | 异常列表 |
| 补卡申请 | POST | /api/v1/attendance/repair | 补卡申请 |
| 补卡审批 | PUT | /api/v1/attendance/repair/{id}/approve | 补卡审批 |
| 导出报表 | GET | /api/v1/attendance/report/export | 导出考勤报表 |

### 3.6 文件服务（file-service）

#### 3.6.1 模块职责

- 文件上传：附件上传到MinIO（白名单校验、文件名随机化）
- 文件管理：列表、预览、下载
- 存储桶按业务类型隔离
- 生命周期管理（自动清理过期文件）

#### 3.6.2 文件安全策略

```
┌─────────────────────────────────────────────────────────┐
│                    文件安全策略                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. 文件类型白名单                                       │
│     ├── 图片: jpg, jpeg, png, gif, webp                 │
│     ├── 文档: pdf, doc, docx, xls, xlsx, ppt, pptx      │
│     ├── 压缩: zip, rar, 7z                              │
│     └── 其他: txt, csv                                  │
│                                                         │
│  2. 文件名随机化                                         │
│     └── UUID重命名，防止路径猜测                        │
│                                                         │
│  3. 存储桶隔离                                           │
│     ├── leave-attach (请假附件)                         │
│     ├── expense-invoice (报销票据)                      │
│     └── common (通用文件)                               │
│                                                         │
│  4. 生命周期规则                                         │
│     └── 6个月前的附件自动清理                            │
│                                                         │
│  5. 访问控制                                             │
│     └── 存储桶设为私有读，通过API鉴权下载                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 3.6.3 数据模型

```sql
-- 文件表
CREATE TABLE oa_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名(UUID)',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    bucket VARCHAR(50) DEFAULT 'common' COMMENT '存储桶',
    business_type VARCHAR(20) COMMENT '业务类型:LEAVE,EXPENSE,ATTENDANCE',
    business_id BIGINT COMMENT '业务ID',
    uploader_id BIGINT NOT NULL COMMENT '上传人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.6.4 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传文件 | POST | /api/v1/file/upload | 上传文件 |
| 文件列表 | GET | /api/v1/file/list | 文件列表 |
| 文件预览 | GET | /api/v1/file/{id}/preview | 预览文件 |
| 下载文件 | GET | /api/v1/file/{id}/download | 下载文件 |
| 删除文件 | DELETE | /api/v1/file/{id} | 删除文件 |

### 3.7 钉钉集成模块（dingtalk-service）

#### 3.7.1 模块职责

- 通讯录同步：部门、用户同步（每天凌晨2点）
- 考勤同步：每日考勤数据同步
- 消息推送：审批结果通知（工作通知）
- 免登认证：钉钉扫码登录
- 定时任务使用ShedLock分布式锁，防止多实例重复执行

#### 3.7.2 同步机制设计

```
┌─────────────────────────────────────────────────────────┐
│                  钉钉数据同步机制                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  定时同步 (每天凌晨2点，使用ShedLock锁)                    │
│       │                                                 │
│       ├── 用户同步 ──▶ sys_user.dingtalk_userid         │
│       ├── 部门同步 ──▶ sys_department.dingtalk_id       │
│       └── 考勤同步 ──▶ oa_attendance                    │
│                                                         │
│  事件回调 (钉钉 → Webhook)                               │
│       │                                                 │
│       ├── 用户变更回调                                    │
│       └── 部门变更回调                                    │
│                                                         │
│  消息推送                                               │
│       │                                                 │
│       └── 工作通知 ──▶ 小程序消息                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 3.7.3 ShedLock定时任务配置

```java
@Configuration
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }
}

// 钉钉同步任务
@SchedulerLock(name = "dingtalkSync", lockAtLeastFor = "5m", lockAtMostFor = "30m")
public void syncDingtalkData() {
    log.info("开始钉钉数据同步...");
    // 同步逻辑
    log.info("钉钉数据同步完成");
}
```

#### 3.7.4 数据模型

```sql
-- 钉钉同步日志表
CREATE TABLE oa_dingtalk_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(20) NOT NULL COMMENT '同步类型:USER,DEPT,ATTENDANCE',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    success_count INT DEFAULT 0 COMMENT '成功数量',
    fail_count INT DEFAULT 0 COMMENT '失败数量',
    error_msg TEXT COMMENT '错误信息',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态:RUNNING,SUCCESS,FAILED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 钉钉配置表
CREATE TABLE oa_dingtalk_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_key VARCHAR(100) NOT NULL COMMENT '应用Key',
    app_secret VARCHAR(200) NOT NULL COMMENT '应用Secret(AES加密存储)',
    agent_id VARCHAR(50) COMMENT 'AgentId',
    corp_id VARCHAR(100) COMMENT '企业ID',
    callback_url VARCHAR(200) COMMENT '回调地址',
    callback_token VARCHAR(100) COMMENT '回调Token',
    callback_aes_key VARCHAR(200) COMMENT '回调AES密钥',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.7.5 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 钉钉登录 | GET | /api/v1/dingtalk/login | 扫码登录 |
| 同步回调 | POST | /api/v1/dingtalk/callback | 事件回调 |
| 手动同步 | POST | /api/v1/dingtalk/sync | 手动触发同步 |
| 同步日志 | GET | /api/v1/dingtalk/sync/logs | 同步日志 |

---

## 4. 数据架构设计

### 4.1 数据库物理设计

| 数据库 | 服务 | 字符集 | 说明 |
|--------|------|--------|------|
| **oa_system** | system | utf8mb4 | 用户、部门、角色 |
| **oa_workflow** | workflow | utf8mb4 | 流程、审批 |
| **oa_collaboration** | collaboration | utf8mb4 | 消息、通讯录 |
| **oa_finance** | finance | utf8mb4 | 报销、预算 |
| **oa_attendance** | attendance | utf8mb4 | 考勤 |
| **oa_common** | file, dingtalk | utf8mb4 | 文件、钉钉 |

### 4.2 主键生成策略

采用雪花算法（Snowflake），由MyBatis-Plus自动生成：

- 时间戳：41位（毫秒级，69年）
- 机器标识：5位（支持32个节点）
- 序列号：12位（每节点每毫秒4096个ID）

**配置：**

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: snowflake
      table-prefix: oa_
```

### 4.3 敏感字段加密

| 字段 | 加密方式 | 存储格式 | 说明 |
|------|----------|----------|------|
| password | BCrypt | 60字符哈希 | 不可逆 |
| mobile | AES-256 | Base64 | 可解密查询 |
| bank_account | AES-256 | Base64 | 可解密查询 |
| dingtalk_app_secret | AES-256 | Base64 | 可解密调用 |

**密钥管理：** AES密钥通过环境变量注入，不硬编码在配置文件中。

```yaml
# 环境变量配置
AES_SECRET_KEY: ${AES_SECRET_KEY}  # 运行时从环境变量读取

# 密钥轮换策略：每半年更换一次，更换时重新加密历史数据
```

### 4.4 缓存策略

#### 4.4.1 缓存层级

```
┌─────────────────────────────────────────────────────────┐
│                    Redis 缓存层级                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  L1: 本地缓存 (Caffeine) - 热数据,50MB                   │
│      └─ 用户信息、权限码                                  │
│                                                         │
│  L2: Redis缓存 - 共享数据,支持分布式                      │
│      └─ 部门树、会话、Token                               │
│                                                         │
│  L3: 数据库 - 最终数据                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 4.4.2 缓存Key规范

| 前缀 | 说明 | 过期时间 |
|------|------|----------|
| `user:{id}` | 用户信息 | 30分钟 |
| `user:token:{token}` | Token信息 | 15分钟 |
| `dept:tree` | 部门树 | 2小时 |
| `role:perms:{id}` | 角色权限 | 1小时 |
| `dict:type:{type}` | 字典数据 | 2小时 |
| `msg:unread:{userId}` | 未读消息数 | 5分钟 |

### 4.5 消息队列设计

| 消息类型 | RoutingKey | 用途 | 死信处理 |
|----------|------------|------|----------|
| 审批通过通知 | approval.notify.pass | 审批通过后通知申请人 | 3次重试后进入死信队列 |
| 审批拒绝通知 | approval.notify.reject | 审批拒绝后通知申请人 | 3次重试后进入死信队列 |
| 消息推送 | message.push.* | 实时推送消息 | 3次重试后进入死信队列 |
| 钉钉同步 | dingtalk.sync.* | 钉钉数据同步 | 3次重试后进入死信队列 |
| 考勤同步 | attendance.sync.* | 考勤数据同步 | 3次重试后进入死信队列 |

---

## 5. API设计

### 5.1 API网关设计

#### 5.1.1 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: system-service
          uri: lb://system-service
          predicates:
            - Path=/api/v1/system/**
          filters:
            - StripPrefix=1

        - id: workflow-service
          uri: lb://workflow-service
          predicates:
            - Path=/api/v1/workflow/**
          filters:
            - StripPrefix=1

        - id: collaboration-service
          uri: lb://collaboration-service
          predicates:
            - Path=/api/v1/collab/**
          filters:
            - StripPrefix=1

        - id: finance-service
          uri: lb://finance-service
          predicates:
            - Path=/api/v1/finance/**
          filters:
            - StripPrefix=1

        - id: attendance-service
          uri: lb://attendance-service
          predicates:
            - Path=/api/v1/attendance/**
          filters:
            - StripPrefix=1

        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/v1/file/**
          filters:
            - StripPrefix=1

        - id: dingtalk-service
          uri: lb://dingtalk-service
          predicates:
            - Path=/api/v1/dingtalk/**
          filters:
            - StripPrefix=1
```

#### 5.1.2 全局过滤器

```java
@Component
public class GlobalFilter implements GlobalFilter {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. JWT验证
        // 2. 限流检查
        // 3. 日志记录
        // 4. 跨域处理
    }
}
```

### 5.2 RESTful API规范

#### 5.2.1 接口规范

| 规则 | 说明 |
|------|------|
| URL格式 | `/api/v1/{module}/{resource}` |
| 命名 | 小写字母、中划线分隔 |
| 版本 | URL路径包含版本号 |
| 复数 | 资源名称使用复数形式 |

**示例：**

| 操作 | 方法 | URL |
|------|------|-----|
| 获取用户列表 | GET | /api/v1/system/users |
| 获取用户详情 | GET | /api/v1/system/users/{id} |
| 创建用户 | POST | /api/v1/system/users |
| 更新用户 | PUT | /api/v1/system/users/{id} |
| 删除用户 | DELETE | /api/v1/system/users/{id} |

#### 5.2.2 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1704067200000
}
```

**错误响应：**

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null,
  "timestamp": 1704067200000,
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ]
}
```

#### 5.2.3 状态码规范

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器错误 |

### 5.3 服务间接口定义（OpenFeign）

```java
// System服务客户端
@FeignClient(name = "system-service", path = "/api/v1/system",
             fallbackFactory = SystemClientFallbackFactory.class)
public interface SystemClient {

    @GetMapping("/user/{id}")
    Result<UserDTO> getUserById(@PathVariable Long id);

    @GetMapping("/user/ids")
    Result<List<UserDTO>> getUsersByIds(@RequestParam List<Long> ids);

    @GetMapping("/dept/tree")
    Result<List<DeptTreeVO>> getDeptTree();

    @GetMapping("/dept/{id}/leader")
    Result<UserDTO> getDeptLeader(@PathVariable Long deptId);

    @GetMapping("/user/{id}/permissions")
    Result<List<String>> getUserPermissions(@PathVariable Long id);
}

// Workflow服务客户端
@FeignClient(name = "workflow-service", path = "/api/v1/workflow",
             fallbackFactory = WorkflowClientFallbackFactory.class)
public interface WorkflowClient {

    @GetMapping("/leave/{id}")
    Result<LeaveDTO> getLeaveById(@PathVariable Long id);

    @PostMapping("/leave")
    Result<Long> createLeave(@RequestBody LeaveForm form);

    @GetMapping("/expense/{id}")
    Result<ExpenseDTO> getExpenseById(@PathVariable Long id);
}

// Message服务客户端
@FeignClient(name = "collaboration-service", path = "/api/v1/collab",
             fallbackFactory = MessageClientFallbackFactory.class)
public interface MessageClient {

    @PostMapping("/messages")
    Result<Void> sendMessage(@RequestBody MessageDTO message);

    @GetMapping("/messages/unread/{userId}")
    Result<Integer> getUnreadCount(@PathVariable Long userId);
}
```

### 5.4 API文档

使用SpringDoc/OpenAPI 3.0生成API文档：

- Swagger UI: `/swagger-ui.html`
- API Docs: `/v3/api-docs`
- Knife4j: `/doc.html`

---

## 6. 安全设计

### 6.1 认证授权

#### 6.1.1 JWT认证流程

```
┌─────────────────────────────────────────────────────────┐
│                    JWT认证流程                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  登录请求                                               │
│      │                                                  │
│      ▼                                                  │
│  ┌──────────────┐                                       │
│  │  验证用户名  │                                       │
│  │  验证密码    │                                       │
│  └──────┬───────┘                                       │
│         │                                              │
│         ▼                                              │
│  生成AccessToken(15分钟) + RefreshToken(7天)            │
│  RefreshToken存储到Redis，支持撤销                      │
│         │                                              │
│         ▼                                              │
│  返回Token给客户端                                       │
│         │                                              │
│         ▼                                              │
│  客户端请求携带AccessToken                               │
│         │                                              │
│         ▼                                              │
│  ┌──────────────┐                                       │
│  │ Gateway验证 │                                       │
│  │ Token解析   │                                       │
│  └──────┬───────┘                                       │
│         │                                              │
│         ▼                                              │
│  权限校验                                               │
│         │                                              │
│         ▼                                              │
│  业务处理                                               │
│                                                         │
│  登出时：Redis删除RefreshToken，实现Token撤销            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 6.1.2 Token设计

```java
public class TokenVO {
    private String accessToken;   // 访问令牌，15分钟有效期
    private String refreshToken;  // 刷新令牌，7天有效期
    private Long expiresIn;        // 过期时间(秒)
    private String tokenType;      // Bearer
}
```

#### 6.1.3 RBAC权限模型

```
┌─────────────────────────────────────────────────────────┐
│                    RBAC权限模型                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  用户 ──关联──▶ 角色 ──关联──▶ 权限                      │
│                                                         │
│  用户1 ◀─────── 普通员工                                │
│                  ├── 请假申请                            │
│                  ├── 费用报销                            │
│                  └── 考勤查看                            │
│                                                         │
│  用户2 ◀─────── 部门经理                                │
│                  ├── 普通员工权限                         │
│                  ├── 审批管理                            │
│                  └── 数据查看(本部门)                    │
│                                                         │
│  用户3 ◀─────── 财务专员                                │
│                  ├── 普通员工权限                         │
│                  ├── 财务审批                            │
│                  └── 报表管理                            │
│                                                         │
│  用户4 ◀─────── 系统管理员                              │
│                  ├── 所有权限                            │
│                  └── 系统管理                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 6.2 接口安全

#### 6.2.1 网关安全过滤器

```java
@Component
public class SecurityFilter implements GlobalFilter {
    // 1. Token验证
    // 2. 限流(滑动窗口算法)
    // 3. 防XSS/CSRF
    // 4. 请求签名(可选)
}
```

#### 6.2.2 限流配置

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200
            redis-rate-limiter.requestedTokens: 1
```

### 6.3 数据安全

#### 6.3.1 敏感字段处理

| 字段 | 处理方式 | 说明 |
|------|----------|------|
| 密码 | BCrypt加密 | 存储哈希值 |
| 手机号 | AES加密存储，显示脱敏 | 138****1234 |
| 银行卡 | AES加密存储，显示脱敏 | ****1234 |
| 身份证 | AES加密存储，显示脱敏 | ************1234 |

#### 6.3.2 审计日志

```sql
-- 操作日志表（敏感字段脱敏）
CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(50) COMMENT '模块',
    business_type VARCHAR(20) COMMENT '业务类型',
    method VARCHAR(100) COMMENT '请求方法',
    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方式',
    request_params TEXT COMMENT '请求参数(脱敏后)',
    response_data TEXT COMMENT '响应数据',
    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户名',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(200) COMMENT '操作地点',
    error_msg TEXT COMMENT '错误信息',
    execute_time BIGINT COMMENT '执行时间(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日志异步写入（MQ）
@Slf4j
@Component
public class AsyncOperLogService {
    @RabbitListener(queues = "queue.oper.log")
    public void saveLog(OperLogDTO log) {
        // 异步保存到数据库
    }
}
```

---

## 7. 部署方案

### 7.1 服务器配置

| 配置项 | 开发环境 | 生产环境（20人） |
|--------|----------|-------------------|
| **CPU** | 4核 | 8核+ |
| **内存** | 8GB | 16GB |
| **硬盘** | 100GB | 500GB+ |
| **操作系统** | Windows/Linux/Mac | Linux（推荐Ubuntu 22.04） |
| **JDK** | 17 | 17 |

### 7.2 服务端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| **Gateway** | 8080 | API网关 |
| **System** | 8081 | 系统服务 |
| **Workflow** | 8082 | 流程服务 |
| **Collaboration** | 8083 | 协作服务 |
| **Finance** | 8084 | 财务服务 |
| **Attendance** | 8085 | 考勤服务 |
| **File** | 8086 | 文件服务 |
| **DingTalk** | 8087 | 钉钉服务 |
| **MySQL** | 3306 | 数据库 |
| **Redis** | 6379 | 缓存 |
| **Nacos** | 8848 | 注册中心 |
| **RabbitMQ** | 5672 | 消息队列 |
| **RabbitMQ Console** | 15672 | 管理界面 |
| **MinIO Console** | 9090 | 文件控制台 |

### 7.3 部署目录结构

```
/opt/solidoa/
├── jdk-17/                          # Java环境
├── mysql/                           # MySQL数据目录
│   ├── data/                        # 数据库文件
│   └── conf/
│       └── my.cnf                   # MySQL配置
├── redis/                           # Redis数据目录
│   └── conf/
│       └── redis.conf               # Redis配置
├── nacos/                           # Nacos服务
│   ├── bin/
│   │   └── startup.sh
│   └── data/
├── rabbitmq/                        # RabbitMQ数据目录
│   └── data/
├── minio/                           # MinIO数据目录
│   └── data/
├── app/                             # 应用目录
│   ├── solidoa-gateway.jar          # 网关服务
│   ├── solidoa-system.jar          # 系统服务
│   ├── solidoa-workflow.jar        # 流程服务
│   ├── solidoa-collaboration.jar   # 协作服务
│   ├── solidoa-finance.jar         # 财务服务
│   ├── solidoa-attendance.jar      # 考勤服务
│   ├── solidoa-file.jar            # 文件服务
│   └── solidoa-dingtalk.jar        # 钉钉服务
├── sql/                             # 数据库脚本
│   ├── init.sql                    # 初始化脚本
│   └── update/                     # 升级脚本
├── scripts/                         # 启动脚本
│   ├── start-all.sh                # 启动所有服务
│   ├── stop-all.sh                 # 停止所有服务
│   └── health-check.sh             # 健康检查
├── logs/                            # 日志目录
│   ├── gateway/
│   ├── system/
│   ├── workflow/
│   ├── collaboration/
│   ├── finance/
│   ├── attendance/
│   ├── file/
│   └── dingtalk/
└── config/                          # 配置文件
    ├── application.yml             # 公共配置
    ├── gateway.yml                 # 网关配置
    ├── system.yml                  # 系统服务配置
    └── ...
```

### 7.4 健康检查启动脚本

```bash
#!/bin/bash
# start-all.sh

# 配置
APP_HOME="/opt/solidoa"
LOG_HOME="${APP_HOME}/logs"
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 等待函数（健康检查）
wait_for_healthy() {
    local service_name=$1
    local port=$2
    local max_wait=60
    local wait_time=0

    echo "等待 ${service_name} (端口 ${port}) 启动..."
    while [ $wait_time -lt $max_wait ]; do
        if curl -s "http://localhost:${port}/actuator/health" | grep -q "UP"; then
            echo "${service_name} 已就绪"
            return 0
        fi
        sleep 2
        wait_time=$((wait_time + 2))
    done
    echo "${service_name} 启动超时"
    return 1
}

# 启动函数
start_service() {
    local service_name=$1
    local service_jar=$2
    local service_port=$3

    echo "启动 ${service_name}..."
    nohup java ${JAVA_OPTS} \
        -jar ${APP_HOME}/app/${service_jar} \
        --spring.config.additional-location=${APP_HOME}/config/ \
        --server.port=${service_port} \
        > ${LOG_HOME}/${service_name}/stdout.log 2>&1 &
    echo "${service_name} 启动中 (PID: $!)"
}

# 创建日志目录
mkdir -p ${LOG_HOME}/*

echo "===== SolidOA 启动开始 ====="

# 启动基础设施（MySQL、Redis需手动启动）

# 等待Nacos启动
echo "等待 Nacos 启动..."
while ! curl -s "http://localhost:8848/nacos/v1/console/health" | grep -q "UP"; do
    echo "Nacos 未就绪，等待 5 秒..."
    sleep 5
done
echo "Nacos 已就绪"

# 启动应用服务
start_service "gateway" "solidoa-gateway.jar" "8080"
start_service "system" "solidoa-system.jar" "8081"
start_service "workflow" "solidoa-workflow.jar" "8082"
start_service "collaboration" "solidoa-collaboration.jar" "8083"
start_service "finance" "solidoa-finance.jar" "8084"
start_service "attendance" "solidoa-attendance.jar" "8085"
start_service "file" "solidoa-file.jar" "8086"
start_service "dingtalk" "solidoa-dingtalk.jar" "8087"

# 健康检查
echo "执行健康检查..."
wait_for_healthy "gateway" "8080"

echo "===== SolidOA 启动完成 ====="
echo "Gateway: http://localhost:8080"
echo "Nacos:   http://localhost:8848/nacos"
echo "MinIO:   http://localhost:9090"
```

### 7.5 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_workflow DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_collaboration DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_finance DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_attendance DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS oa_common DEFAULT CHARACTER SET utf8mb4;

-- 执行初始化脚本
-- mysql -u root -p < /opt/solidoa/sql/init.sql
```

---

## 8. 监控与运维

### 8.1 监控架构

```
┌─────────────────────────────────────────────────────────┐
│                    监控架构                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  应用层                                                  │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │
│  │ System  │ │Workflow │ │Finance  │ │  ...    │       │
│  │Service  │ │Service  │ │Service  │ │         │       │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘       │
│       │           │           │           │             │
│       └───────────┴───────────┴───────────┘             │
│                    │                                    │
│                    ▼                                    │
│  指标采集                                                  │
│  ┌─────────────────────────────────────────┐           │
│  │    Spring Boot Actuator + Micrometer    │           │
│  │         /actuator/prometheus            │           │
│  └─────────────────────────────────────────┘           │
│                    │                                    │
│                    ▼                                    │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   │
│  │ Prometheus  │──▶│  Grafana    │──▶│   Dashboard │   │
│  │   时序数据库  │   │  可视化大屏  │   │   监控面板   │   │
│  └─────────────┘   └─────────────┘   └─────────────┘   │
│                                                         │
│  日志聚合                                                  │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   │
│  │    Loki     │──▶│  Grafana    │──▶│   日志查询   │   │
│  │   日志存储   │   │  日志面板    │   │             │   │
│  └─────────────┘   └─────────────┘   └─────────────┘   │
│                                                         │
│  告警通知                                                  │
│  ┌─────────────┐   ┌─────────────┐                      │
│  │AlertManager │──▶│  钉钉/邮件   │                      │
│  │   告警规则   │   │   通知      │                      │
│  └─────────────┘   └─────────────┘                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 8.2 监控指标

#### 8.2.1 应用指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| JVM堆内存使用率 | 堆内存使用百分比 | > 80% |
| JVM GC频率 | Full GC次数 | > 1次/小时 |
| HTTP请求延迟 | P99延迟 | > 2秒 |
| 接口错误率 | 5xx错误占比 | > 1% |
| 数据库连接池 | 活跃连接数 | > 80% |

#### 8.2.2 中间件指标

| 组件 | 指标 | 告警阈值 |
|------|------|----------|
| MySQL | 连接数、慢查询 | > 100慢查询/分钟 |
| Redis | 内存使用、连接数 | > 80%内存 |
| RabbitMQ | 队列堆积、消费者断开 | > 1000消息堆积 |
| Nacos | 注册实例数 | < 预期实例数 |

### 8.3 备份与灾难恢复

#### 8.3.1 备份策略

| 备份类型 | 频率 | 保留周期 | 备份内容 |
|----------|------|----------|----------|
| 全量备份 | 每日凌晨3点 | 30天 | MySQL所有库 |
| 增量备份 | 每小时 | 7天 | MySQL binlog |
| Redis备份 | 每日凌晨3点 | 7天 | RDB快照 |
| 文件备份 | 每周日 | 4周 | MinIO对象存储 |
| 配置备份 | 每次变更 | 30天 | Nacos配置 |

#### 8.3.2 备份脚本

```bash
#!/bin/bash
# backup.sh

BACKUP_HOME="/opt/solidoa/backup"
DATE=$(date +%Y%m%d)
TIME=$(date +%H%M%S)

# MySQL全量备份
mysqldump -u root -p${DB_PASSWORD} --all-databases > ${BACKUP_HOME}/mysql/${DATE}_${TIME}.sql

# Redis备份
redis-cli SAVE

# 备份到异地（rsync到备份服务器）
rsync -avz ${BACKUP_HOME} backup-server:/backup/solidoa/

# 清理30天前的备份
find ${BACKUP_HOME} -mtime +30 -delete
```

#### 8.3.3 恢复演练

- 每季度执行一次完整恢复演练
- 验证备份完整性和恢复时间
- 恢复时间目标：RTO < 4小时

### 8.4 运维命令

```bash
# 查看服务状态
curl http://localhost:8080/actuator/health

# 查看日志
tail -f /opt/solidoa/logs/system/stdout.log

# 重启服务
ps aux | grep solidoa-system.jar | awk '{print $2}' | xargs kill -9
nohup java -jar /opt/solidoa/app/solidoa-system.jar &

# 查看线程dump
jstack <pid> > threaddump.log

# 查看堆内存
jmap -heap <pid> > heap.log
```

---

## 9. 开发规范

### 9.1 项目结构

```
solidoa/
├── solidoa-parent/                 # 父POM
│   ├── pom.xml
│   └── modules/
│       ├── solidoa-common/        # 公共模块
│       │   ├── pom.xml
│       │   └── src/main/java/
│       │       ├── config/        # 配置类
│       │       ├── constant/      # 常量
│       │       ├── enums/        # 枚举
│       │       ├── exception/     # 异常
│       │       ├── result/        # 统一响应
│       │       └── util/          # 工具类
│       │
│       ├── solidoa-gateway/       # 网关
│       ├── solidoa-system/        # 系统服务
│       ├── solidoa-workflow/      # 流程服务
│       ├── solidoa-collaboration/ # 协作服务
│       ├── solidoa-finance/       # 财务服务
│       ├── solidoa-attendance/    # 考勤服务
│       ├── solidoa-file/          # 文件服务
│       └── solidoa-dingtalk/      # 钉钉服务
│
├── solidoa-web/                   # Vue3前端
│   ├── src/
│   │   ├── api/                   # API定义
│   │   ├── components/            # 公共组件
│   │   ├── layouts/              # 布局
│   │   ├── router/               # 路由
│   │   ├── stores/               # Pinia状态
│   │   ├── utils/                # 工具
│   │   ├── views/                # 页面
│   │   │   ├── system/           # 系统管理
│   │   │   ├── workflow/         # 审批流程
│   │   │   ├── collab/           # 协作办公
│   │   │   ├── finance/          # 财务管理
│   │   │   ├── attendance/       # 考勤管理
│   │   │   └── report/          # 报表
│   │   └── App.vue
│   └── package.json
│
└── sql/                           # 数据库脚本
    ├── init.sql
    └── update/
```

### 9.2 代码规范

| 规范 | 说明 |
|------|------|
| **命名** | 类名PascalCase，方法/变量camelCase，常量UPPER_SNAKE_CASE |
| **注释** | 类和方法添加Javadoc，复杂逻辑添加行内注释 |
| **分层** | Controller → Service → Mapper/Dao → Entity |
| **异常** | 统一异常处理，业务异常使用自定义异常 |
| **日志** | 使用@Slf4j，记录关键操作和异常 |
| **敏感数据** | 审计日志中脱敏处理 |

### 9.3 Git分支规范

```
main                    # 主分支(生产)
├── develop             # 开发分支
│   ├── feature/xxx     # 功能分支
│   ├── fix/xxx         # 修复分支
│   └── refactor/xxx    # 重构分支
└── release/1.0         # 发布分支
```

### 9.4 API版本规范

| 版本 | URL | 说明 |
|------|-----|------|
| V1 | /api/v1/xxx | 当前稳定版本 |
| V2 | /api/v2/xxx | 未来版本 |

### 9.5 性能指标

| 指标 | 要求 | 说明 |
|------|------|------|
| 页面响应时间 | < 2秒 | 正常网络环境 |
| API响应时间(缓存命中) | < 100ms | 普通查询 |
| API响应时间(缓存未命中) | < 500ms | 普通查询 |
| 复杂报表响应时间 | < 2秒 | 允许导出异步化 |
| 并发用户数 | ≥ 50 | 峰值并发 |
| 数据库查询 | < 1秒 | 复杂查询 |
| 文件上传 | < 10秒 | 10MB以内 |
| 缓存命中率 | ≥ 80% | 热点数据 |

### 9.6 测试策略

| 测试类型 | 覆盖率目标 | 说明 |
|----------|------------|------|
| 单元测试 | ≥ 70% | 核心业务逻辑 |
| 集成测试 | 关键路径 | 使用Testcontainers |
| 性能测试 | JMeter | 50并发模拟 |

---

## 10. 开发计划

### 10.1 项目阶段

```
┌─────────────────────────────────────────────────────────────────┐
│                      SolidOA 开发计划                            │
├─────────────────────────────────────────────────────────────────┤
│                                                              │
│  Phase 1: 基础架构（4周）                                      │
│  ├── 项目脚手架（Git + Maven多模块）                          │
│  ├── 服务注册中心（Nacos）                                    │
│  ├── API网关（Gateway）                                       │
│  ├── 数据库设计                                               │
│  └── 系统管理（用户、部门、角色）                               │
│                                                              │
│  Phase 2: 核心业务（8周）                                     │
│  ├── 审批流程（Camunda + 请假审批）                           │
│  ├── 费用报销                                                │
│  ├── 消息中心                                                │
│  ├── 通讯录                                                  │
│  └── 日程管理                                                │
│                                                              │
│  Phase 3: 扩展功能（2周）                                     │
│  ├── 考勤模块                                                │
│  ├── 钉钉集成                                                │
│  └── 文件管理                                                │
│                                                              │
│  Phase 4: 移动端（4周）                                       │
│  ├── UniApp框架                                              │
│  ├── 审批处理                                                │
│  ├── 考勤打卡                                                │
│  └── 消息通知（APP用WebSocket，小程序用钉钉工作通知）           │
│                                                              │
│  Phase 5: 测试上线（2周）                                     │
│  ├── 功能测试                                                │
│  ├── 性能测试                                                │
│  ├── 部署上线                                                │
│  └── 文档编写                                                │
│                                                              │
└─────────────────────────────────────────────────────────────────┘
```

### 10.2 工时估算

| 模块 | 功能点 | 工时（人天） |
|------|--------|---------------|
| **基础架构** | 项目搭建、Nacos、Gateway、数据库 | 20 |
| **系统管理** | 用户、部门、角色、权限 | 15 |
| **审批流程** | Camunda、请假、报销、加签转交 | 25 |
| **协作办公** | 消息、通讯录、日程、WebSocket | 15 |
| **财务模块** | 报销、预算、报表 | 15 |
| **考勤模块** | 打卡、统计、钉钉同步 | 15 |
| **文件服务** | 上传、预览、下载、安全校验 | 10 |
| **钉钉集成** | 免登、同步、消息推送 | 10 |
| **移动端** | UniApp、iOS/Android/小程序 | 35 |
| **测试上线** | 测试、部署、文档 | 20 |
| **总计** | - | **180人天** |

### 10.3 里程碑

| 里程碑 | 日期 | 交付内容 |
|--------|------|----------|
| **M1** | 第4周 | 基础架构完成，可登录系统 |
| **M2** | 第8周 | 核心审批流程可用 |
| **M3** | 第12周 | 协作办公模块完成 |
| **M4** | 第14周 | 移动端APP上线 |
| **M5** | 第16周 | 正式发布 |

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| SSD | System Specification Document，系统规格说明书 |
| RBAC | Role-Based Access Control，基于角色的访问控制 |
| JWT | JSON Web Token，JSON网络令牌 |
| BPMN | Business Process Model and Notation，业务流程模型和符号 |
| Camunda | 开源工作流引擎 |
| Nacos | 阿里开源的服务注册与配置中心 |
| MinIO | S3兼容的对象存储服务器 |
| UniApp | 跨平台前端框架 |
| OpenFeign | 声明式HTTP客户端 |
| Snowflake | 雪花算法，分布式ID生成器 |
| Sentinel | 熔断限流组件 |
| ShedLock | 分布式定时任务锁 |
| DLX | Dead Letter Exchange，死信交换机 |

### B. 环境配置示例

**application.yml (通用配置)：**

```yaml
server:
  port: ${PORT:8080}
  servlet:
    context-path: /

spring:
  application:
    name: ${APP_NAME:solidoa-service}

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:oa_system}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root123}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:solidoa}
    password: ${RABBITMQ_PASSWORD:solidoa123}

  cloud:
    nacos:
      server-addr: ${NACOS_HOST:localhost}:${NACOS_PORT:8848}
      username: nacos
      password: nacos

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.solidoa.**.entity
  global-config:
    db-config:
      id-type: snowflake
      table-prefix: oa_
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取
  expiration: 900       # 15分钟
  refresh-expiration: 604800  # 7天

aes:
  secret-key: ${AES_SECRET_KEY}  # 从环境变量读取

logging:
  level:
    com.solidoa: DEBUG
  file:
    name: ${LOG_PATH:/opt/solidoa/logs}/${spring.application.name}/app.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

**文档版本历史**

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| V1.0 | 2026-05-24 | 初始版本，完整SSD设计 | SolidOA Architecture Team |
| V1.1 | 2026-05-24 | 补充熔断降级、死信队列、ShedLock、监控方案、性能指标、工时修正 | SolidOA Architecture Team |
| V1.2 | 2026-05-26 | 新增用印申请模块(3.2.7)和采购申请模块(3.2.8)详细设计 | SolidOA Architecture Team |