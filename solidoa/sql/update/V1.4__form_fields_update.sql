-- =====================================================
-- SolidOA 表单字段补全脚本 V1.4
-- 更新日期: 2026-05-27
-- =====================================================

-- ==================== 1. 补卡申请表 oa_repair_card ====================
-- 添加 time_slot 字段（时间段：MORNING上班、NIGHT下班）
ALTER TABLE `oa_repair_card`
ADD COLUMN `time_slot` VARCHAR(20) DEFAULT NULL COMMENT '时间段：MORNING上班、NIGHT下班' AFTER `reason`;

-- ==================== 2. 外出申请表 oa_go_out（新建） ====================
CREATE TABLE IF NOT EXISTS `oa_go_out` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `out_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '外出单号',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `dept_id` BIGINT COMMENT '部门ID',
    `out_date` DATE NOT NULL COMMENT '外出日期',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `destination` VARCHAR(200) NOT NULL COMMENT '外出地点',
    `reason` VARCHAR(500) NOT NULL COMMENT '外出事由',
    `attachments` VARCHAR(500) COMMENT '附件',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING待审批、APPROVED已通过、REJECTED已拒绝、CANCELLED已撤回',
    `process_instance_id` VARCHAR(100) COMMENT 'Camunda流程实例ID',
    `current_approver_id` BIGINT COMMENT '当前审批人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_out_date` (`out_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_current_approver` (`current_approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外出申请表';

-- ==================== 3. 出差申请表 oa_business_trip ====================
-- 添加 companions 字段（随行人员）
ALTER TABLE `oa_business_trip`
ADD COLUMN `companions` VARCHAR(500) DEFAULT NULL COMMENT '随行人员' AFTER `budget_remark`;

-- ==================== 4. 采购申请表 oa_purchase ====================
-- 添加 requester_dept_id（需求部门）、apply_date（申请日期）、spec（规格型号）
ALTER TABLE `oa_purchase`
ADD COLUMN `requester_dept_id` BIGINT DEFAULT NULL COMMENT '需求部门ID' AFTER `dept_id`,
ADD COLUMN `apply_date` DATE DEFAULT NULL COMMENT '申请日期' AFTER `requester_dept_id`,
ADD COLUMN `spec` VARCHAR(200) DEFAULT NULL COMMENT '规格型号' AFTER `quantity`;

-- ==================== 5. 报销申请表 oa_expense ====================
-- 大幅增强字段
ALTER TABLE `oa_expense`
DROP COLUMN IF EXISTS `amount`,
DROP COLUMN IF EXISTS `process_instance_id`,
ADD COLUMN `trip_report_title` VARCHAR(200) DEFAULT NULL COMMENT '出差报告标题' AFTER `expense_type`,
ADD COLUMN `trip_start_date` DATE DEFAULT NULL COMMENT '出差开始日期' AFTER `trip_report_title`,
ADD COLUMN `trip_end_date` DATE DEFAULT NULL COMMENT '出差结束日期' AFTER `trip_start_date`,
ADD COLUMN `trip_days` DECIMAL(5,1) DEFAULT NULL COMMENT '出差天数' AFTER `trip_end_date`,
ADD COLUMN `transport_fee` DECIMAL(10,2) DEFAULT 0 COMMENT '交通费' AFTER `trip_days`,
ADD COLUMN `hotel_fee` DECIMAL(10,2) DEFAULT 0 COMMENT '住宿费' AFTER `transport_fee`,
ADD COLUMN `other_fee` DECIMAL(10,2) DEFAULT 0 COMMENT '其他费用' AFTER `hotel_fee`,
ADD COLUMN `total_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '合计金额' AFTER `other_fee`,
ADD COLUMN `account_name` VARCHAR(50) DEFAULT NULL COMMENT '收款户名' AFTER `other_fee`,
ADD COLUMN `approver_comment` TEXT DEFAULT NULL COMMENT '审批意见' AFTER `status`,
ADD COLUMN `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID' AFTER `approver_comment`,
ADD COLUMN `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间' AFTER `approver_id`;

-- ==================== 6. 工资审批表 oa_salary_approval ====================
-- 完全重建表结构
DROP TABLE IF EXISTS `oa_salary_approval`;

CREATE TABLE IF NOT EXISTS `oa_salary_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `salary_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '工资单号',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名',
    `dept_id` BIGINT DEFAULT NULL COMMENT '部门ID',
    `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    `year_month` VARCHAR(7) NOT NULL COMMENT '工资月份',
    `basic_salary` DECIMAL(10,2) DEFAULT NULL COMMENT '基本工资',
    `position_salary` DECIMAL(10,2) DEFAULT NULL COMMENT '岗位工资',
    `full_attendance_bonus` DECIMAL(10,2) DEFAULT 0 COMMENT '全勤奖',
    `overtime_pay` DECIMAL(10,2) DEFAULT 0 COMMENT '加班费',
    `deductions` DECIMAL(10,2) DEFAULT 0 COMMENT '扣款合计',
    `gross_salary` DECIMAL(10,2) DEFAULT NULL COMMENT '应发工资',
    `net_salary` DECIMAL(10,2) DEFAULT NULL COMMENT '实发工资',
    `approver_comment` TEXT DEFAULT NULL COMMENT '审批意见',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿、PENDING待审批、APPROVED已通过、REJECTED已拒绝',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_year_month` (`year_month`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资审批表';

-- ==================== 7. 用印申请表 oa_stamp ====================
-- 修改用印类型、添加用印次数字段
ALTER TABLE `oa_stamp`
MODIFY COLUMN `stamp_type` VARCHAR(20) NOT NULL COMMENT '用印类型：PUBLIC公章、CONTRACT合同章、FINANCIAL财务章、LEGAL法人章',
ADD COLUMN `stamp_count` INT DEFAULT 1 COMMENT '用印次数' AFTER `usage`;

-- ==================== 完成 ====================
SELECT '表单字段补全脚本执行完成' AS result;