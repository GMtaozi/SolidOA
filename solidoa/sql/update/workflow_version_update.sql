-- ==========================================
-- SolidOA Workflow 模块数据库变更脚本
-- 执行前请备份数据库
-- ==========================================

-- 切换到 workflow 数据库
USE oa_workflow;

-- oa_leave 表变更
ALTER TABLE oa_leave ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER current_approver_id;
ALTER TABLE oa_leave ADD INDEX idx_current_approver (current_approver_id);

-- oa_expense 表变更
ALTER TABLE oa_expense ADD COLUMN current_approver_id BIGINT COMMENT '当前审批人ID' AFTER process_instance_id;
ALTER TABLE oa_expense ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER current_approver_id;
ALTER TABLE oa_expense ADD INDEX idx_current_approver (current_approver_id);

-- 验证变更
SHOW COLUMNS FROM oa_leave;
SHOW COLUMNS FROM oa_expense;