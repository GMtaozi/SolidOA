-- ==========================================
-- SolidOA 数据库初始化数据
-- ==========================================

-- 插入管理员用户 (密码: admin123，BCrypt加密)
-- BCrypt hash for "admin123": $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
INSERT INTO sys_user (username, password, real_name, dept_id, status) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '系统管理员', 1, 1);

-- 插入角色
INSERT INTO sys_role (name, code, description) VALUES
('系统管理员', 'SYSTEM_ADMIN', '系统最高权限'),
('部门经理', 'DEPT_MANAGER', '部门负责人'),
('财务专员', 'FINANCE', '财务人员'),
('普通员工', 'EMPLOYEE', '普通员工'),
('考勤管理员', 'ATTENDANCE_ADMIN', '考勤管理员');

-- 插入部门
INSERT INTO sys_department (name, parent_id, sort) VALUES
('总公司', 0, 0),
('技术部', 1, 1),
('销售部', 1, 2),
('财务部', 1, 3),
('行政部', 1, 4),
('开发组', 2, 1),
('测试组', 2, 2);

-- 插入数据字典
INSERT INTO sys_dict (type, label, value, sort) VALUES
('leave_type', '年假', 'ANNUAL', 1),
('leave_type', '病假', 'SICK', 2),
('leave_type', '事假', 'PERSONAL', 3),
('leave_type', '调休', 'COMPENSATORY', 4),
('leave_type', '出差', 'BUSINESS', 5),
('expense_type', '差旅费', 'TRAVEL', 1),
('expense_type', '办公费', 'OFFICE', 2),
('expense_type', '业务招待费', 'ENTERTAINMENT', 3),
('expense_type', '其他', 'OTHER', 4);

-- 插入审批流程配置
INSERT INTO oa_approval_flow_config (business_type, flow_name, is_default, config) VALUES
('STAMP', '用印默认流程', TRUE, '{"businessType":"STAMP","flowName":"用印默认流程","isDefault":true,"nodes":[{"order":1,"name":"部门负责人审批","approverType":"FIXED","approverId":1,"approverName":"系统管理员","mode":"ANY"}]}');

-- 给管理员分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);