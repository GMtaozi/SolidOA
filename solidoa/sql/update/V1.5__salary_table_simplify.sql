-- ==========================================
-- SolidOA 工资单表结构更新 V1.5
-- 日期：2026-05-27
-- 说明：简化工资单表结构，只保留审批流程相关字段
-- ==========================================

-- 重新创建 oa_salary 表
DROP TABLE IF EXISTS oa_salary;

CREATE TABLE oa_salary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_no VARCHAR(32) NOT NULL UNIQUE COMMENT '工资单号',

    -- 基本信息
    user_id BIGINT COMMENT '申请人ID',
    user_name VARCHAR(50) COMMENT '申请人姓名',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(100) COMMENT '部门名称',
    apply_date DATE COMMENT '申请日期',

    -- 工资信息
    salary_month VARCHAR(7) NOT NULL COMMENT '薪资期间(YYYY-MM)',
    pay_date DATE COMMENT '发薪日期',
    pay_type VARCHAR(20) COMMENT '发薪类型:MONTHLY月薪,BONUS奖金,ANNUAL年终奖,OTHER其他',

    -- 汇总数据
    employee_count INT DEFAULT 0 COMMENT '工资发放人数',
    total_gross_salary DECIMAL(14,2) DEFAULT 0 COMMENT '应发工资合计',
    total_deduction DECIMAL(14,2) DEFAULT 0 COMMENT '代扣代缴合计',
    total_net_salary DECIMAL(14,2) DEFAULT 0 COMMENT '实发工资合计',

    -- 附件
    attachments VARCHAR(500) COMMENT '附件',

    -- 状态
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT草稿,PENDING待审批,APPROVED已通过,REJECTED已驳回,PAID已发放,CANCELLED已撤回',

    -- 审批信息
    approver_id BIGINT COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    approve_time DATETIME COMMENT '审批时间',
    approve_comment VARCHAR(500) COMMENT '审批意见',

    -- 发放信息
    paid_time DATETIME COMMENT '发放时间',

    -- 审计字段
    creator_id BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_salary_no (salary_no),
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资单主表';