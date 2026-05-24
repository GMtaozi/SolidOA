# SolidOA 系统规格说明书（SSD）

> 版本：V1.0
> 日期：2026-05-24
> 作者：SolidOA Architecture Team
> 状态：草稿

---

## 目录

1. [系统概述](#1-系统概述)
2. [技术架构设计](#2-技术架构设计)
3. [核心模块设计](#3-核心模块设计)
4. [数据架构设计](#4-数据架构设计)
5. [API设计](#5-api设计)
6. [安全设计](#6-安全设计)
7. [部署方案](#7-部署方案)
8. [开发规范](#8-开发规范)

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
│                   统一入口 · JWT认证 · 路由转发 · 限流                      │
├──────────────────────────────┼────────────────────────────────────────────┤
│                         服务层 (Microservices)                             │
│                                                                         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────┐ │
│  │ System  │ │Workflow │ │ Colla   │ │ Finance │ │  File   │ │ Ding  │ │
│  │ :8081   │ │ :8082   │ │ :8083   │ │ :8084   │ │ :8085   │ │ :8086 │ │
│  │ 用户    │ │ Camunda │ │ 消息    │ │ 报销    │ │ MinIO   │ │ 同步  │ │
│  │ 部门    │ │ 审批    │ │ 通讯录  │ │ 预算    │ │ 文件    │ │ 推送  │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └───┬───┘ │
│       │           │           │           │           │         │     │
│       └───────────┴───────────┴───────────┴───────────┴─────────┘     │
│                              │                                            │
│              ┌───────────────┴───────────────┐                         │
│              │         消息队列层             │                         │
│              │       RabbitMQ :5672          │                         │
│              │  Exchange: solidoa.exchange   │                         │
│              └───────────────────────────────┘                         │
├──────────────────────────────┼────────────────────────────────────────────┤
│                              │                                            │
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
| **gateway** | 8080 | API网关、认证、路由、限流 | - | Spring Cloud Gateway |
| **system** | 8081 | 用户、部门、角色、权限 | oa_system | Spring Boot + MyBatis-Plus |
| **workflow** | 8082 | Camunda流程引擎、审批管理 | oa_workflow | Spring Boot + Camunda |
| **collaboration** | 8083 | 消息、日程、通讯录 | oa_collaboration | Spring Boot + WebSocket |
| **finance** | 8084 | 报销、预算、报表 | oa_finance | Spring Boot |
| **attendance** | 8085 | 考勤打卡、统计、钉钉同步 | oa_attendance | Spring Boot |
| **file** | 8086 | 文件上传、下载、预览 | oa_file | Spring Boot + MinIO |
| **dingtalk** | 8087 | 钉钉同步、免登、消息推送 | oa_dingtalk | Spring Boot + 钉钉SDK |

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

#### 2.4.1 同步通信（OpenFeign）

```
┌─────────────────────────────────────────────────────────┐
│                    同步HTTP调用                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Client (发起调用)                                        │
│       │                                                  │
│       ├── SystemClient ────▶ System Service (8081)      │
│       │              获取用户/部门信息                    │
│       │                                                  │
│       ├── WorkflowClient ──▶ Workflow Service (8082)    │
│       │              提交/查询审批                        │
│       │                                                  │
│       └── FinanceClient ──▶ Finance Service (8084)      │
│                  获取报销/预算数据                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**OpenFeign Client定义示例：**

```java
@FeignClient(name = "system-service", path = "/api/v1/system")
public interface SystemClient {

    @GetMapping("/user/{id}")
    UserDTO getUserById(@PathVariable Long id);

    @GetMapping("/dept/tree")
    List<DeptTreeVO> getDeptTree();
}
```

#### 2.4.2 异步通信（RabbitMQ）

```
┌─────────────────────────────────────────────────────────┐
│                    异步消息通知                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Exchange: solidoa.exchange (Topic)                     │
│       │                                                 │
│       ├── queue.approval.notify ──▶ 审批通过/拒绝通知     │
│       ├── queue.message.push ────▶ 消息中心推送          │
│       ├── queue.dingtalk.sync ───▶ 钉钉数据同步          │
│       └── queue.attendance.sync ──▶ 考勤数据同步          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**RabbitMQ配置：**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: solidoa
    password: solidoa123
    virtual-host: /solidoa

  cloud:
    stream:
      bindings:
        approval-notify:
          destination: solidoa.approval
          content-type: application/json
        message-push:
          destination: solidoa.message
          content-type: application/json
```

### 2.5 技术选型汇总

#### 后端技术栈

| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| **微服务框架** | Spring Boot | 3.2.x | 应用框架 |
| **微服务治理** | Spring Cloud Alibaba | 2023.x | 服务治理 |
| **服务注册** | Nacos | 2.2.x | 注册+配置中心 |
| **网关** | Spring Cloud Gateway | 4.x | API网关 |
| **ORM** | MyBatis-Plus | 3.5.x | 数据访问 |
| **工作流** | Camunda 7.x + 自研状态机 | 7.19 | 复杂流程+简单审批 |
| **安全** | Spring Security + JWT | - | 认证授权 |
| **缓存** | Redis | 7.x | 分布式缓存/会话 |
| **消息队列** | RabbitMQ | 3.x | 异步通知 |
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
| user:{id} | 30分钟 | 用户信息 |
| user:username:{username} | 30分钟 | 用户名查询 |
| dept:tree | 2小时 | 部门树 |
| dept:{id} | 1小时 | 部门详情 |
| permission:role:{id} | 1小时 | 角色权限 |
| dict:type:{type} | 2小时 | 字典数据 |

### 3.2 审批流程模块（workflow-service）

#### 3.2.1 模块职责

- 工作流引擎：Camunda管理复杂审批流程
- 审批管理：请假、报销、通用审批
- 加签/转交：自研状态机处理
- 催办提醒：RabbitMQ消息通知

#### 3.2.2 Camunda流程设计

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

**BPMN流程定义（简化示例）：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  id="leave_process" name="请假审批流程">

  <bpmn:process id="leaveProcess" name="请假审批" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
    <bpmn:userTask id="ApplyTask" name="填写申请" assignee="${initiator}"/>
    <bpmn:userTask id="ManagerApprove" name="直属审批" candidateGroups="DEPT_MANAGER"/>
    <bpmn:exclusiveGateway id="Gateway_1" name="是否需要二级审批"/>
    <bpmn:userTask id="SeniorApprove" name="二级审批" candidateGroups="SENIOR_MANAGER"/>
    <bpmn:endEvent id="EndEvent_1" name="结束"/>
    <bpmn:sequenceFlow sourceRef="StartEvent_1" targetRef="ApplyTask"/>
    <bpmn:sequenceFlow sourceRef="ApplyTask" targetRef="ManagerApprove"/>
    <bpmn:sequenceFlow sourceRef="ManagerApprove" targetRef="Gateway_1"/>
    <!-- 根据请假天数判断是否需要二级审批 -->
    <bpmn:sequenceFlow id="Flow_yes" name="≥3天" sourceRef="Gateway_1" targetRef="SeniorApprove">
      <bpmn:conditionExpression>${days >= 3}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_no" name="<3天" sourceRef="Gateway_1" targetRef="EndEvent_1"/>
  </bpmn:process>
</bpmn:definitions>
```

#### 3.2.3 自研状态机（加签/转交）

```java
public enum ApprovalStatus {
    PENDING,     // 待审批
    APPROVED,    // 已同意
    REJECTED,    // 已拒绝
    TRANSFERRED, // 已转交
    ADD_SIGN,    // 加签中
    CANCELLED,   // 已撤回
    COMPLETED    // 已完成
}

public class ApprovalStateMachine {
    // 状态流转规则
    // PENDING -> APPROVED (同意)
    // PENDING -> REJECTED (拒绝)
    // PENDING -> TRANSFERRED (转交)
    // PENDING -> ADD_SIGN (加签)
    // PENDING -> CANCELLED (撤回)
    // APPROVED -> COMPLETED (流程结束)
}
```

#### 3.2.4 数据模型

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

-- 审批记录表
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型:LEAVE,EXPENSE',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approve_type VARCHAR(20) NOT NULL COMMENT '审批类型:APPROVE同意,REJECT拒绝,TRANSFER转交,ADD_SIGN加签',
    comment TEXT COMMENT '审批意见',
    task_id VARCHAR(100) COMMENT 'Camunda任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
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

#### 3.2.5 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 新建请假 | POST | /api/v1/workflow/leave | 创建请假申请 |
| 请假列表 | GET | /api/v1/workflow/leave | 分页查询请假 |
| 请假详情 | GET | /api/v1/workflow/leave/{id} | 请假详情 |
| 审批请假 | POST | /api/v1/workflow/leave/{id}/approve | 审批请假 |
| 撤回请假 | PUT | /api/v1/workflow/leave/{id}/cancel | 撤回请假 |
| 催办 | POST | /api/v1/workflow/leave/{id}/remind | 催办 |
| 新建报销 | POST | /api/v1/workflow/expense | 创建报销申请 |
| 报销列表 | GET | /api/v1/workflow/expense | 分页查询报销 |
| 审批报销 | POST | /api/v1/workflow/expense/{id}/approve | 审批报销 |
| 待我审批 | GET | /api/v1/workflow/tasks/pending | 待审批列表 |
| 我已审批 | GET | /api/v1/workflow/tasks/approved | 已审批列表 |
| 我发起的 | GET | /api/v1/workflow/tasks/my-apply | 我发起的 |

### 3.3 协作办公模块（collaboration-service）

#### 3.3.1 模块职责

- 消息中心：系统通知、审批通知、实时推送
- 通讯录：员工列表、搜索、拨号
- 日程管理：创建日程、提醒、分享

#### 3.3.2 WebSocket实时推送

```
┌─────────────────────────────────────────────────────────┐
│                  WebSocket 消息推送                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  客户端 ────▶ Gateway(WebSocket) ◀─── RabbitMQ          │
│                  │                    │                 │
│                  │                    │                 │
│              路由到               消息通知               │
│         collaboration-service      审批结果             │
│                  │                    │                 │
│                  ▼                    ▼                 │
│            ┌──────────────────────────────────┐         │
│            │      WebSocket Session          │         │
│            │  - user:1001 -> session:abc      │         │
│            │  - user:1002 -> session:def      │         │
│            └──────────────────────────────────┘         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**WebSocket配置：**

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageHandler(), "/ws/message/{userId}")
                .setAllowedOrigins("*");
    }
}
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
    INDEX idx_receiver (receiver_id, is_read),
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

- 打卡记录：员工考勤打卡
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

- 文件上传：附件上传到MinIO
- 文件管理：列表、预览、下载
- 权限控制：文件访问权限

#### 3.6.2 数据模型

```sql
-- 文件表
CREATE TABLE oa_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    bucket VARCHAR(50) DEFAULT 'solidoa' COMMENT '存储桶',
    business_type VARCHAR(20) COMMENT '业务类型:LEAVE,EXPENSE,ATTENDANCE',
    business_id BIGINT COMMENT '业务ID',
    uploader_id BIGINT NOT NULL COMMENT '上传人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3.6.3 API接口设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传文件 | POST | /api/v1/file/upload | 上传文件 |
| 文件列表 | GET | /api/v1/file/list | 文件列表 |
| 文件预览 | GET | /api/v1/file/{id}/preview | 预览文件 |
| 下载文件 | GET | /api/v1/file/{id}/download | 下载文件 |
| 删除文件 | DELETE | /api/v1/file/{id} | 删除文件 |

### 3.7 钉钉集成模块（dingtalk-service）

#### 3.7.1 模块职责

- 通讯录同步：部门、用户同步
- 考勤同步：每日考勤数据同步
- 消息推送：审批结果通知
- 免登认证：钉钉扫码登录

#### 3.7.2 同步机制设计

```
┌─────────────────────────────────────────────────────────┐
│                  钉钉数据同步机制                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  定时同步 (每天凌晨2点)                                   │
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
└─────────────────────────────────────────────────────────┘
```

#### 3.7.3 数据模型

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
    app_secret VARCHAR(200) NOT NULL COMMENT '应用Secret(AES加密)',
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

#### 3.7.4 API接口设计

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

**加密工具类：**

```java
@Component
public class AesEncryptor {
    @Encrypt AesDecrypt aesDecrypt = new AesDecryptHandler();

    public String encrypt(String plainText) {
        // AES-256-GCM加密
    }

    public String decrypt(String cipherText) {
        // AES-256-GCM解密
    }
}
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
| `dict:{type}` | 字典数据 | 2小时 |
| `msg:unread:{userId}` | 未读消息数 | 5分钟 |

### 4.5 消息队列设计

#### 4.5.1 Exchange和Queue设计

```yaml
spring:
  rabbitmq:
    exchanges:
      - name: solidoa.exchange
        type: topic
        durable: true

    queues:
      approval-notify:
        name: queue.approval.notify
        durable: true
        bindings:
          - exchange: solidoa.exchange
            routingKey: approval.notify.*
      message-push:
        name: queue.message.push
        durable: true
        bindings:
          - exchange: solidoa.exchange
            routingKey: message.push.*
      dingtalk-sync:
        name: queue.dingtalk.sync
        durable: true
        bindings:
          - exchange: solidoa.exchange
            routingKey: dingtalk.sync.*
```

#### 4.5.2 消息类型

| 消息类型 | RoutingKey | 用途 |
|----------|------------|------|
| 审批通过通知 | approval.notify.pass | 审批通过后通知申请人 |
| 审批拒绝通知 | approval.notify.reject | 审批拒绝后通知申请人 |
| 消息推送 | message.push.* | 实时推送消息 |
| 钉钉同步 | dingtalk.sync.* | 钉钉数据同步 |

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
    // 1. JWT验证
    // 2. 限流
    // 3. 日志记录
    // 4. 跨域处理
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
| 500 | 服务器错误 |

### 5.3 服务间接口定义（OpenFeign）

```java
// System服务客户端
@FeignClient(name = "system-service", path = "/api/v1/system")
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
@FeignClient(name = "workflow-service", path = "/api/v1/workflow")
public interface WorkflowClient {

    @GetMapping("/leave/{id}")
    Result<LeaveDTO> getLeaveById(@PathVariable Long id);

    @PostMapping("/leave")
    Result<Long> createLeave(@RequestBody LeaveForm form);

    @GetMapping("/expense/{id}")
    Result<ExpenseDTO> getExpenseById(@PathVariable Long id);
}

// Message服务客户端
@FeignClient(name = "collaboration-service", path = "/api/v1/collab")
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
-- 操作日志表
CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(50) COMMENT '模块',
    business_type VARCHAR(20) COMMENT '业务类型',
    method VARCHAR(100) COMMENT '请求方法',
    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方式',
    request_params TEXT COMMENT '请求参数',
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

### 7.4 启动脚本

```bash
#!/bin/bash
# start-all.sh

# 配置
APP_HOME="/opt/solidoa"
LOG_HOME="${APP_HOME}/logs"
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动函数
start_service() {
    local service_name=$1
    local service_jar=$2
    local service_port=$3

    echo "Starting ${service_name}..."
    nohup java ${JAVA_OPTS} \
        -jar ${APP_HOME}/app/${service_jar} \
        --spring.config.additional-location=${APP_HOME}/config/ \
        --server.port=${service_port} \
        > ${LOG_HOME}/${service_name}/stdout.log 2>&1 &

    echo "${service_name} started (PID: $!)"
}

# 创建日志目录
mkdir -p ${LOG_HOME}/*

echo "===== SolidOA 启动开始 ====="

# 启动应用服务
start_service "gateway" "solidoa-gateway.jar" "8080"
start_service "system" "solidoa-system.jar" "8081"
start_service "workflow" "solidoa-workflow.jar" "8082"
start_service "collaboration" "solidoa-collaboration.jar" "8083"
start_service "finance" "solidoa-finance.jar" "8084"
start_service "attendance" "solidoa-attendance.jar" "8085"
start_service "file" "solidoa-file.jar" "8086"
start_service "dingtalk" "solidoa-dingtalk.jar" "8087"

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

## 8. 开发规范

### 8.1 项目结构

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

### 8.2 代码规范

| 规范 | 说明 |
|------|------|
| **命名** | 类名PascalCase，方法/变量camelCase，常量UPPER_SNAKE_CASE |
| **注释** | 类和方法添加Javadoc，复杂逻辑添加行内注释 |
| **分层** | Controller → Service → Mapper/Dao → Entity |
| **异常** | 统一异常处理，业务异常使用自定义异常 |
| **日志** | 使用@Slf4j，记录关键操作和异常 |

### 8.3 Git分支规范

```
main                    # 主分支(生产)
├── develop             # 开发分支
│   ├── feature/xxx     # 功能分支
│   ├── fix/xxx         # 修复分支
│   └── refactor/xxx    # 重构分支
└── release/1.0         # 发布分支
```

### 8.4 API版本规范

| 版本 | URL | 说明 |
|------|-----|------|
| V1 | /api/v1/xxx | 当前稳定版本 |
| V2 | /api/v2/xxx | 未来版本 |

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
  secret: ${JWT_SECRET:solidoa-secret-key-change-in-production}
  expiration: 900       # 15分钟
  refresh-expiration: 604800  # 7天

logging:
  level:
    com.solidoa: DEBUG
  file:
    name: ${LOG_PATH:/opt/solidoa/logs}/${spring.application.name}/app.log
```

---

**文档版本历史**

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| V1.0 | 2026-05-24 | 初始版本，完整SSD设计 | SolidOA Architecture Team |