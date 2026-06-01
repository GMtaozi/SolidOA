-- V1.5__add_repair_unique_constraint
-- 为补卡申请表添加唯一约束，防止并发重复提交
-- 约束：同一用户、同一日期、同一补卡类型只能有一条待审批或已审批记录
ALTER TABLE oa_repair_card
ADD CONSTRAINT uk_repair_unique UNIQUE (user_id, repair_date, repair_type, status);