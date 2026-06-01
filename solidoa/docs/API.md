# SolidOA API 文档

## 基础信息

- **Base URL**: `http://localhost:8080` (通过网关) 或直接访问各服务
- **认证方式**: Header `X-User-Id` (用户ID)
- **Content-Type**: `application/json`

## 服务端口

| 服务 | 端口 | Base Path |
|------|------|-----------|
| Gateway | 8080 | /api/v1 |
| System | 8081 | /api/v1/system |
| Workflow | 8082 | /api/v1/workflow |
| Collaboration | 8083 | /api/v1/collab |
| Finance | 8084 | /api/v1/finance |
| Attendance | 8085 | /api/v1/attendance |
| File | 8086 | /api/v1/file |

---

## 系统管理 (System - 8081)

### 用户管理

#### 获取用户列表
```
GET /api/v1/system/users
```

#### 获取用户详情
```
GET /api/v1/system/users/{id}
```

#### 创建用户
```
POST /api/v1/system/users
Body: {
    "username": "string",
    "realName": "string",
    "password": "string",
    "mobile": "string",
    "email": "string",
    "deptId": number,
    "position": "string"
}
```

#### 更新用户
```
PUT /api/v1/system/users/{id}
Body: {
    "realName": "string",
    "mobile": "string",
    "email": "string",
    "deptId": number,
    "position": "string"
}
```

#### 修改密码
```
PUT /api/v1/system/users/{id}/password
Body: {
    "oldPassword": "string",
    "newPassword": "string"
}
```

#### 删除用户
```
DELETE /api/v1/system/users/{id}
```

### 部门管理

#### 获取部门树
```
GET /api/v1/system/depts/tree
```

#### 获取部门列表
```
GET /api/v1/system/depts
```

#### 创建部门
```
POST /api/v1/system/depts
Body: {
    "parentId": number,
    "deptName": "string",
    "deptCode": "string",
    "leader": "string",
    "mobile": "string",
    "sort": number
}
```

#### 更新部门
```
PUT /api/v1/system/depts/{id}
```

#### 删除部门
```
DELETE /api/v1/system/depts/{id}
```

### 角色管理

#### 获取角色列表
```
GET /api/v1/system/roles
```

#### 获取角色权限
```
GET /api/v1/system/roles/{id}/permissions
```

#### 创建角色
```
POST /api/v1/system/roles
Body: {
    "roleName": "string",
    "roleCode": "string",
    "description": "string",
    "permissionIds": [number]
}
```

#### 更新角色权限
```
PUT /api/v1/system/roles/{id}/permissions
Body: {
    "permissionIds": [number]
}
```

### 数据字典

#### 获取字典类型列表
```
GET /api/v1/system/dicts
```

#### 获取字典项
```
GET /api/v1/system/dicts/types/{typeCode}
```

### 日程管理

#### 创建日程
```
POST /api/v1/system/schedules
Header: X-User-Id: {userId}
Body: {
    "title": "string",
    "content": "string",
    "location": "string",
    "startTime": "datetime",
    "endTime": "datetime",
    "isAllDay": number,
    "remindBefore": number,
    "remindWay": "string",
    "color": "string"
}
```

#### 更新日程
```
PUT /api/v1/system/schedules/{id}
Header: X-User-Id: {userId}
Body: { ... }
```

#### 删除日程
```
DELETE /api/v1/system/schedules/{id}
Header: X-User-Id: {userId}
```

#### 获取日程详情
```
GET /api/v1/system/schedules/{id}
```

#### 获取日程列表
```
GET /api/v1/system/schedules?startDate={datetime}&endDate={datetime}
Header: X-User-Id: {userId}
```

#### 获取所有日程（管理员）
```
GET /api/v1/system/schedules/all?startDate={datetime}&endDate={datetime}
```

### 通讯录

#### 创建联系人
```
POST /api/v1/system/contacts
Body: {
    "userId": number,
    "deptId": number,
    "deptName": "string",
    "realName": "string",
    "mobile": "string",
    "email": "string",
    "position": "string",
    "avatar": "string"
}
```

#### 更新联系人
```
PUT /api/v1/system/contacts/{id}
Body: { ... }
```

#### 删除联系人
```
DELETE /api/v1/system/contacts/{id}
```

#### 获取联系人详情
```
GET /api/v1/system/contacts/{id}
```

#### 获取联系人列表
```
GET /api/v1/system/contacts
```

#### 按部门获取联系人
```
GET /api/v1/system/contacts/dept/{deptId}
```

#### 搜索联系人
```
GET /api/v1/system/contacts/search?keyword={keyword}&pageNum=1&pageSize=20
```

---

## 审批流程 (Workflow - 8082)

### 请假申请

#### 创建请假
```
POST /api/v1/workflow/leave
Header: X-User-Id: {userId}
Body: {
    "leaveType": "string",
    "startTime": "datetime",
    "endTime": "datetime",
    "duration": number,
    "reason": "string",
    "attachments": "string"
}
```

#### 获取请假详情
```
GET /api/v1/workflow/leave/{id}
```

#### 取消请假
```
PUT /api/v1/workflow/leave/{id}/cancel
Header: X-User-Id: {userId}
```

### 报销申请

#### 创建报销
```
POST /api/v1/workflow/expense
Header: X-User-Id: {userId}
Body: {
    "expenseType": "string",
    "amount": number,
    "description": "string",
    "attachments": "string"
}
```

#### 获取报销详情
```
GET /api/v1/workflow/expense/{id}
```

#### 确认付款
```
POST /api/v1/workflow/expense/{id}/confirm-payment
```

### 审批任务

#### 获取待处理任务
```
GET /api/v1/workflow/tasks/pending
Header: X-User-Id: {userId}
```

#### 获取已处理任务
```
GET /api/v1/workflow/tasks/processed
Header: X-User-Id: {userId}
```

#### 审批任务
```
POST /api/v1/workflow/tasks/{taskId}/approve
Header: X-User-Id: {userId}
Body: {
    "approved": boolean,
    "comment": "string"
}
```

#### 获取任务概要
```
GET /api/v1/workflow/tasks/summary-data
```

---

## 协作办公 (Collaboration - 8083)

### 消息通知

#### 发送消息
```
POST /api/v1/collab/messages
Header: X-User-Id: {userId}
Body: {
    "receiverId": number,
    "content": "string",
    "type": "string",
    "priority": number
}
```

#### 获取消息列表
```
GET /api/v1/collab/messages
Header: X-User-Id: {userId}
```

#### 获取未读消息数
```
GET /api/v1/collab/messages/unread-count
Header: X-User-Id: {userId}
```

#### 标记消息已读
```
PUT /api/v1/collab/messages/{id}/read
Header: X-User-Id: {userId}
```

#### 标记全部已读
```
PUT /api/v1/collab/messages/read-all
Header: X-User-Id: {userId}
```

---

## 财务管理 (Finance - 8084)

### 预算管理

#### 获取预算列表
```
GET /api/v1/finance/budget
Query: year={year}&month={month}
```

#### 创建预算
```
POST /api/v1/finance/budget
Body: {
    "deptId": number,
    "year": number,
    "month": number,
    "totalAmount": number,
    "warningThreshold": number
}
```

#### 更新预算
```
PUT /api/v1/finance/budget
Body: { ... }
```

#### 调整预算
```
POST /api/v1/finance/budget/adjust
Body: {
    "budgetId": number,
    "amount": number,
    "adjustType": "ADD|SUBTRACT",
    "reason": "string"
}
```

#### 获取预算预警
```
GET /api/v1/finance/budget/warnings
```

### 报销统计

#### 获取报销统计
```
GET /api/v1/finance/report/expense
Query: startDate={date}&endDate={date}
```

#### 按部门获取报销
```
GET /api/v1/finance/report/dept-expense
Query: startDate={date}&endDate={date}
```

#### 导出报销
```
POST /api/v1/finance/report/export
Body: {
    "year": number,
    "month": number
}
```

---

## 考勤管理 (Attendance - 8085)

### 打卡签到

#### 签到
```
POST /api/v1/attendance/check
Header: X-User-Id: {userId}
Body: {
    "checkType": "SIGN_IN|SIGN_OUT",
    "location": "string",
    "deviceInfo": "string"
}
```

#### 获取打卡异常
```
GET /api/v1/attendance/exceptions
Header: X-User-Id: {userId}
Query: startDate={date}&endDate={date}
```

#### 获取月度汇总
```
GET /api/v1/attendance/month-summary
Header: X-User-Id: {userId}
Query: year={year}&month={month}
```

### 补卡申请

#### 创建补卡
```
POST /api/v1/attendance/repair
Header: X-User-Id: {userId}
Body: {
    "userId": number,
    "repairDate": "date",
    "repairType": "SIGN_IN|SIGN_OUT",
    "repairTime": "datetime",
    "reason": "string",
    "attachments": "string"
}
```

#### 获取补卡待审批列表
```
GET /api/v1/attendance/repair/pending
```

#### 审批补卡
```
PUT /api/v1/attendance/repair/{id}/approve
Header: X-User-Id: {userId}
Body: {
    "approved": boolean,
    "comment": "string"
}
```

---

## 文件服务 (File - 8086)

### 文件上传
```
POST /api/v1/file/upload
Content-Type: multipart/form-data
Body: {
    "file": (binary),
    "bucket": "string"
}
```

### 文件下载
```
GET /api/v1/file/{id}/download
```

### 文件预览
```
GET /api/v1/file/{id}/preview
```

### 获取文件列表
```
GET /api/v1/file?bucket={bucket}&pageNum=1&pageSize=20
```

### 删除文件
```
DELETE /api/v1/file/{id}
```

---

## 通用响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {},
    "timestamp": 1234567890123
}
```

### 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

---

## 数据类型说明

### 日期时间格式
- ISO 8601: `2026-05-25T10:00:00`
- 或: `2026-05-25 10:00:00`

### 金额
- 单位: 元
- 类型: number

### 状态值
- 请假类型: `ANNUAL`(年假), `SICK`(病假), `PERSONAL`(事假), `MARRIAGE`(婚假), `MATERNITY`(产假)
- 报销类型: `TRAVEL`(差旅), `ENTERTAINMENT`(招待), `OFFICE`(办公), `OTHER`(其他)
- 审批状态: `PENDING`(待审批), `APPROVED`(已通过), `REJECTED`(已拒绝)
- 打卡类型: `SIGN_IN`(签到), `SIGN_OUT`(签退)
