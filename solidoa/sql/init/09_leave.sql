-- oa_leave_type 假期类型表
CREATE TABLE oa_leave_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_code VARCHAR(20) NOT NULL UNIQUE COMMENT '假期编码',
    leave_name VARCHAR(50) NOT NULL COMMENT '假期名称',
    default_days DECIMAL(5,1) DEFAULT 0 COMMENT '默认天数',
    max_days DECIMAL(5,1) DEFAULT 0 COMMENT '最大天数(0=无限制)',
    requires_proof TINYINT DEFAULT 0 COMMENT '是否需要证明材料',
    can_transfer TINYINT DEFAULT 0 COMMENT '是否可结转',
    transfer_max_days DECIMAL(5,1) DEFAULT 0 COMMENT '可结转最大天数',
    valid_months INT DEFAULT 0 COMMENT '有效期(月),0=永久有效',
    deduct_salary TINYINT DEFAULT 0 COMMENT '是否扣工资',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_leave_balance 员工假期余额表
CREATE TABLE oa_leave_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '假期类型',
    year INT NOT NULL COMMENT '年度',
    total_days DECIMAL(5,1) DEFAULT 0 COMMENT '可用天数',
    used_days DECIMAL(5,1) DEFAULT 0 COMMENT '已使用天数',
    pending_days DECIMAL(5,1) DEFAULT 0 COMMENT '待审批天数',
    transferred_days DECIMAL(5,1) DEFAULT 0 COMMENT '已结转天数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type_year (user_id, leave_type, year),
    INDEX idx_user_id (user_id),
    INDEX idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化假期类型数据
INSERT INTO oa_leave_type (leave_code, leave_name, default_days, max_days, requires_proof, can_transfer, transfer_max_days, valid_months, deduct_salary, sort, status) VALUES
('ANNUAL', '年假', 5, 20, 0, 1, 5, 12, 0, 1, 1),
('SICK', '病假', 5, 30, 1, 0, 0, 6, 0, 2, 1),
('PERSONAL', '事假', 3, 10, 0, 0, 0, 0, 1, 3, 1),
('MARRIAGE', '婚假', 3, 15, 1, 0, 0, 0, 0, 4, 1),
('MATERNITY', '产假', 90, 180, 1, 0, 0, 0, 0, 5, 1),
('PATERNITY', '陪产假', 10, 30, 1, 0, 0, 0, 0, 6, 1),
('FUNERAL', '丧假', 3, 7, 1, 0, 0, 0, 0, 7, 1),
('WORK_INJURY', '工伤假', 0, 0, 1, 0, 0, 0, 0, 8, 1);