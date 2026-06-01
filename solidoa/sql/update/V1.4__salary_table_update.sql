-- ==========================================
-- SolidOA 工资单表结构更新
-- 版本：V1.4
-- 日期：2026-05-27
-- 说明：更新 oa_salary 表结构以匹配工资审批表单
-- ==========================================

-- 修改 oa_salary 表结构
ALTER TABLE oa_salary
  MODIFY COLUMN basic_salary DECIMAL(10,2) DEFAULT 0 COMMENT '基本工资',
  MODIFY COLUMN position_salary DECIMAL(10,2) DEFAULT 0 COMMENT '岗位工资',
  MODIFY COLUMN performance_salary DECIMAL(10,2) DEFAULT 0 COMMENT '绩效工资',
  MODIFY COLUMN gross_salary DECIMAL(10,2) DEFAULT 0 COMMENT '应发合计',
  MODIFY COLUMN social_security DECIMAL(10,2) DEFAULT 0 COMMENT '社保合计(养老+医疗+失业+公积金)',
  MODIFY COLUMN housing_fund DECIMAL(10,2) DEFAULT 0 COMMENT '住房公积金(单独保留便于统计)',
  MODIFY COLUMN personal_tax DECIMAL(10,2) DEFAULT 0 COMMENT '所得税',
  MODIFY COLUMN net_salary DECIMAL(10,2) DEFAULT 0 COMMENT '实发工资',
  MODIFY COLUMN bank_card_no VARCHAR(50) COMMENT '银行卡号',
  MODIFY COLUMN bank_name VARCHAR(100) COMMENT '开户行',
  MODIFY COLUMN payment_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT草稿,PENDING待审批,APPROVED已通过,REJECTED已驳回,PAID已发放,CANCELLED已撤回';

-- 添加新字段
ALTER TABLE oa_salary
  ADD COLUMN apply_date DATE COMMENT '申请日期' AFTER salary_month,
  ADD COLUMN pension DECIMAL(10,2) DEFAULT 0 COMMENT '养老保险' AFTER performance_salary,
  ADD COLUMN medical DECIMAL(10,2) DEFAULT 0 COMMENT '医疗保险' AFTER pension,
  ADD COLUMN unemployment DECIMAL(10,2) DEFAULT 0 COMMENT '失业保险' AFTER medical,
  ADD COLUMN social_total DECIMAL(10,2) DEFAULT 0 COMMENT '社保合计' AFTER unemployment,
  ADD COLUMN taxable_salary DECIMAL(10,2) DEFAULT 0 COMMENT '计税工资' AFTER social_total;

-- 删除不再使用的字段
ALTER TABLE oa_salary
  DROP COLUMN overtime_pay,
  DROP COLUMN bonus,
  DROP COLUMN commission,
  DROP COLUMN allowances,
  DROP COLUMN other_income,
  DROP COLUMN other_deductions;

-- 更新索引（如果需要）
DROP INDEX idx_payment_status ON oa_salary;
CREATE INDEX idx_status ON oa_salary (payment_status);