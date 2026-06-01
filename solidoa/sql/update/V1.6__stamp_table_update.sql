-- ==========================================
-- SolidOA 用印申请表结构更新 V1.6
-- 日期：2026-05-27
-- 说明：补充用印申请表单字段
-- ==========================================

-- ==================== 用印模块 oa_stamp ====================

-- 添加用印日期字段
ALTER TABLE `oa_stamp` ADD COLUMN `stamp_date` DATE DEFAULT NULL COMMENT '用印日期' AFTER `dept_id`;

-- 添加文件类别字段
ALTER TABLE `oa_stamp` ADD COLUMN `document_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类别' AFTER `document_count`;

-- 添加用印次数字段（如果不存在）
ALTER TABLE `oa_stamp` ADD COLUMN `stamp_count` INT DEFAULT 1 COMMENT '用印次数' AFTER `stamp_usage`;

-- 添加备注字段
ALTER TABLE `oa_stamp` ADD COLUMN `remark` TEXT DEFAULT NULL COMMENT '备注' AFTER `received_by`;

-- ==================== 完成 ====================
SELECT '用印申请表字段补全脚本执行完成' AS result;