-- oa_attendance 打卡记录表
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

-- oa_attendance_summary 考勤统计表
CREATE TABLE oa_attendance_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT COMMENT '部门ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '统计月份',
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
    UNIQUE KEY uk_user_month (user_id, `year_month`),
    INDEX idx_dept_month (dept_id, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_repair_card 补卡申请表
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