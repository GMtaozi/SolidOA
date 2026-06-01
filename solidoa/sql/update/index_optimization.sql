-- =====================================================
-- SolidOA 数据库索引优化
-- 补充高频查询所需的索引
-- =====================================================

-- =====================================================
-- 1. workflow 模块补充索引
-- =====================================================

-- oa_leave: 用户+状态组合查询（我的请假列表）
ALTER TABLE oa_leave ADD INDEX idx_user_status (user_id, status);
-- 原有: idx_user_id, idx_status - 新增组合索引覆盖最常用查询

-- oa_leave: 日期范围查询（考勤统计）
ALTER TABLE oa_leave ADD INDEX idx_date_range (start_date, end_date, user_id);

-- oa_expense: 用户+状态组合查询
ALTER TABLE oa_expense ADD INDEX idx_user_status (user_id, status);
-- 原有: idx_user_id, idx_status - 新增组合索引

-- oa_expense: 金额排序（财务统计）
ALTER TABLE oa_expense ADD INDEX idx_amount_status (amount, status);

-- oa_approval_record: 审批人+时间查询（审批人的历史审批）
ALTER TABLE oa_approval_record ADD INDEX idx_approver_time (approver_id, create_time DESC);

-- oa_transfer_record: 转交记录查询
ALTER TABLE oa_transfer_record ADD INDEX idx_to_approver (to_approver_id, create_time DESC);

-- oa_reminder_record: 催办记录查询
ALTER TABLE oa_reminder_record ADD INDEX idx_approver_last (approver_id, last_reminder_time);

-- =====================================================
-- 2. collaboration 模块补充索引
-- =====================================================

-- oa_message: 按类型查询（消息筛选）
ALTER TABLE oa_message ADD INDEX idx_type_receiver (type, receiver_id);

-- oa_message: 未读消息统计（高频）
ALTER TABLE oa_message ADD INDEX idx_unread_count (receiver_id, is_read, create_time DESC);

-- oa_schedule: 日历视图查询（某时间段内的日程）
ALTER TABLE oa_schedule ADD INDEX idx_time_range (user_id, start_time, end_time);

-- oa_schedule: 按状态查询
ALTER TABLE oa_schedule ADD INDEX idx_user_status (user_id, status);

-- =====================================================
-- 3. attendance 模块补充索引
-- =====================================================

-- oa_attendance: 月度考勤统计（高频）
ALTER TABLE oa_attendance ADD INDEX idx_user_month (user_id, check_date);

-- oa_attendance: 迟到统计
ALTER TABLE oa_attendance ADD INDEX idx_late_user (user_id, is_late, check_date);

-- oa_repair_card: 补卡申请查询
ALTER TABLE oa_repair_card ADD INDEX idx_user_repair_date (user_id, repair_date, status);

-- =====================================================
-- 4. 通用索引（跨模块使用）
-- =====================================================

-- sys_user: 登录名查询
ALTER TABLE sys_user ADD INDEX idx_username (username);

-- sys_user: 部门+状态（获取部门用户）
ALTER TABLE sys_user ADD INDEX idx_dept_status (dept_id, status);

-- sys_department: 父部门查询
ALTER TABLE oa_department ADD INDEX idx_parent_id (parent_id);

-- =====================================================
-- 5. 消息队列补偿表索引
-- =====================================================

-- sys_message_outbox: 补偿任务查询
ALTER TABLE sys_message_outbox ADD INDEX idx_retry (status, next_retry_time, retry_count);

-- =====================================================
-- 6. 审计日志表（如果存在）
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(50) COMMENT '模块名称',
    operation VARCHAR(50) COMMENT '操作类型',
    method VARCHAR(100) COMMENT '请求方法',
    url VARCHAR(255) COMMENT '请求URL',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_id BIGINT COMMENT '操作用户ID',
    user_name VARCHAR(50) COMMENT '操作用户名',
    params TEXT COMMENT '请求参数',
    result VARCHAR(10) COMMENT '操作结果:SUCCESS,FAIL',
    error_msg TEXT COMMENT '错误信息',
    execute_time BIGINT COMMENT '执行时长(ms)',
    trace_id VARCHAR(64) COMMENT '追踪ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, create_time DESC),
    INDEX idx_module_time (module, create_time DESC),
    INDEX idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =====================================================
-- 验证索引创建
-- =====================================================
-- SHOW INDEX FROM oa_leave;
-- SHOW INDEX FROM oa_message;
-- SHOW INDEX FROM oa_attendance;