-- 加班申请表
CREATE TABLE IF NOT EXISTS oa_overtime (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    overtime_no VARCHAR(32) NOT NULL UNIQUE COMMENT '加班单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    overtime_type VARCHAR(20) NOT NULL COMMENT '加班类型:WORKDAY,REST_DAY,HOLIDAY',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    hours DECIMAL(5,1) NOT NULL COMMENT '加班时长(小时)',
    reason TEXT COMMENT '加班原因',
    compensation_type VARCHAR(20) DEFAULT 'BREAK' COMMENT '补偿方式:BREAK调休,SALARY加班费',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING,APPROVED,REJECTED,COMPLETED,CANCELLED',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    approved_hours DECIMAL(5,1) COMMENT '审批通过的时长',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_overtime_type (overtime_type),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班申请表';

-- 加班调休记录表
CREATE TABLE IF NOT EXISTS oa_overtime_break (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    overtime_id BIGINT NOT NULL COMMENT '加班记录ID',
    available_hours DECIMAL(5,1) NOT NULL COMMENT '可用调休时长',
    used_hours DECIMAL(5,1) DEFAULT 0 COMMENT '已使用时长',
    expired_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_expired_time (expired_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班调休记录表';