-- 印章管理表
CREATE TABLE IF NOT EXISTS `sys_stamp` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '印章ID',
    `name` VARCHAR(100) NOT NULL COMMENT '印章名称',
    `type` VARCHAR(50) NOT NULL COMMENT '印章类型：公章/合同专用章/财务专用章/人事专用章/部门章',
    `keeper_id` BIGINT COMMENT '保管人ID',
    `dept_id` BIGINT COMMENT '保管部门ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0停用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_keeper_id` (`keeper_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='印章管理表';

-- 工资条确认字段
ALTER TABLE `biz_salary_slip` ADD COLUMN `confirmed` TINYINT DEFAULT 0 COMMENT '是否确认：0未确认 1已确认' AFTER `status`;
ALTER TABLE `biz_salary_slip` ADD COLUMN `confirm_time` DATETIME COMMENT '确认时间' AFTER `confirmed`;
ALTER TABLE `biz_salary_slip` ADD COLUMN `dispute_reason` VARCHAR(500) COMMENT '异议原因' AFTER `confirm_time`;
ALTER TABLE `biz_salary_slip` ADD COLUMN `dispute_time` DATETIME COMMENT '异议时间' AFTER `dispute_reason`;
ALTER TABLE `biz_salary_slip` ADD COLUMN `dispute_status` VARCHAR(20) COMMENT '异议状态：PENDING/RESOLVED' AFTER `dispute_reason`;

-- 用印记录表
CREATE TABLE IF NOT EXISTS `biz_stamp_usage_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `usage_no` VARCHAR(32) NOT NULL COMMENT '用印单号',
    `stamp_id` BIGINT COMMENT '印章ID',
    `stamp_name` VARCHAR(100) COMMENT '印章名称',
    `applicant_id` BIGINT COMMENT '申请人ID',
    `applicant_name` VARCHAR(50) COMMENT '申请人姓名',
    `dept_name` VARCHAR(100) COMMENT '部门名称',
    `file_name` VARCHAR(255) COMMENT '文件名称',
    `file_count` INT DEFAULT 1 COMMENT '份数',
    `reason` VARCHAR(500) COMMENT '用印事由',
    `usage_time` DATETIME COMMENT '用印时间',
    `operator_id` BIGINT COMMENT '经办人ID',
    `operator_name` VARCHAR(50) COMMENT '经办人姓名',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_usage_no` (`usage_no`),
    KEY `idx_stamp_id` (`stamp_id`),
    KEY `idx_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用印记录表';
