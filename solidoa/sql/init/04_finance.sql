-- oa_budget 预算表
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

-- oa_budget_adjust 预算调整记录表
CREATE TABLE oa_budget_adjust (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    budget_id BIGINT NOT NULL COMMENT '预算ID',
    adjust_amount DECIMAL(12,2) NOT NULL COMMENT '调整金额',
    adjust_type VARCHAR(20) NOT NULL COMMENT '调整类型:ADD增,BACK减',
    reason VARCHAR(200) COMMENT '调整原因',
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_expense_detail 报销明细表
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

-- oa_payment_log 付款操作日志表
CREATE TABLE oa_payment_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expense_id BIGINT NOT NULL COMMENT '报销单ID',
    cashier_id BIGINT NOT NULL COMMENT '出纳ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '付款金额',
    confirm_note VARCHAR(500) COMMENT '确认备注',
    payment_time DATETIME NOT NULL COMMENT '付款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expense_id (expense_id),
    INDEX idx_cashier_id (cashier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;