-- ==========================================
-- SolidOA 考勤与工资相关初始化数据
-- 版本：V1.3
-- 日期：2026-05-26
-- 说明：假期类型和工资项配置的初始化数据
-- ==========================================

-- ==========================================
-- 1. 假期类型初始化数据
-- ==========================================

INSERT INTO oa_leave_type (type_code, type_name, type_name_en, category, paid_type, default_days, max_days, min_days, max_days_once, require_attachment, require_proof_days, approval_required, color, icon, sort, status, remark) VALUES
-- 年假
('ANNUAL', '年假', 'Annual Leave', 'ANNUAL', 'FULL_PAID', 5, 15, 1, 10, 0, 5, 1, '#52C41A', 'calendar', 1, 1, '工作满1年享5天，每增加1年多1天，上限15天'),
-- 病假
('SICK', '病假', 'Sick Leave', 'SICK', 'HALF_PAID', 12, 24, 0.5, 7, 0, 3, 1, '#FF4D4F', 'medicine-box', 2, 1, '全年累计不超过24天，超过3天需提供医院证明'),
-- 事假
('PERSONAL', '事假', 'Personal Leave', 'PERSONAL', 'UNPAID', 0, 20, 0.5, 5, 0, 0, 1, '#FA8C16', 'schedule', 3, 1, '全年累计不超过20天'),
-- 婚假
('MARRIAGE', '婚假', 'Marriage Leave', 'SPECIAL', 'FULL_PAID', 3, 3, 1, 3, 1, 0, 1, '#F5222D', 'heart', 4, 1, '法定婚假3天，需提供结婚证'),
-- 产假
('MATERNITY', '产假', 'Maternity Leave', 'SPECIAL', 'FULL_PAID', 98, 98, 1, 98, 1, 0, 1, '#F759AB', 'baby', 5, 1, '顺产98天，难产增加15天，需提供准生证'),
-- 陪产假
('PATERNITY', '陪产假', 'Paternity Leave', 'SPECIAL', 'FULL_PAID', 10, 10, 1, 10, 1, 0, 1, '#722ED1', 'team', 6, 1, '配偶生育可享受10天陪产假，需提供相关证明'),
-- 丧假
('FUNERAL', '丧假', 'Bereavement Leave', 'SPECIAL', 'FULL_PAID', 3, 5, 1, 5, 0, 0, 1, '#8C8C8C', 'file-text', 7, 1, '父母、配偶、子女死亡3天，祖父母、外祖父母死亡2天，其他1天'),
-- 工伤假
('WORK_INJURY', '工伤假', 'Work Injury Leave', 'SICK', 'FULL_PAID', 0, 365, 0.5, 90, 1, 0, 1, '#FAAD14', 'alert', 8, 1, '因工负伤需提供工伤认定证明，最长不超过365天');

-- ==========================================
-- 2. 工资项配置初始化数据
-- ==========================================

INSERT INTO oa_salary_item (item_code, item_name, item_type, category, is_system, is_required, default_value, calculation_type, sort, visible_in_slip, status, remark) VALUES

-- ========== 收入项 ==========
-- 基本工资
('BASIC_SALARY', '基本工资', 'EARNING', '工资', 1, 1, 0, 'FIXED', 1, 1, 1, '员工的基本薪资'),
-- 岗位工资
('POSITION_SALARY', '岗位工资', 'EARNING', '工资', 1, 0, 0, 'FIXED', 2, 1, 1, '根据岗位级别确定的工资'),
-- 绩效工资
('PERFORMANCE_SALARY', '绩效工资', 'EARNING', '工资', 1, 0, 0, 'CONDITION', 3, 1, 1, '根据绩效考核结果发放'),
-- 加班费
('OVERTIME_PAY', '加班费', 'EARNING', '补贴', 1, 0, 0, 'FORMULA', 4, 1, 1, '按加班时长和加班费率计算'),
-- 全勤奖
('ATTENDANCE_BONUS', '全勤奖', 'EARNING', '奖金', 0, 0, 200, 'CONDITION', 5, 1, 1, '当月无迟到早退旷工'),
-- 餐补
('MEAL_SUBSIDY', '餐补', 'EARNING', '补贴', 0, 0, 0, 'FIXED', 6, 1, 1, '工作餐补贴'),
-- 交通补贴
('TRANSPORT_SUBSIDY', '交通补贴', 'EARNING', '补贴', 0, 0, 0, 'FIXED', 7, 1, 1, '上下班交通补贴'),
-- 通讯补贴
('COMMUNICATION_SUBSIDY', '通讯补贴', 'EARNING', '补贴', 0, 0, 0, 'FIXED', 8, 1, 1, '工作通讯费用补贴'),
-- 住房补贴
('HOUSING_SUBSIDY', '住房补贴', 'EARNING', '补贴', 0, 0, 0, 'FIXED', 9, 1, 1, '住房相关补贴'),
-- 奖金
('BONUS', '奖金', 'EARNING', '奖金', 1, 0, 0, 'FORMULA', 10, 1, 1, '月度/季度/年度奖金'),
-- 提成
('COMMISSION', '提成', 'EARNING', '奖金', 0, 0, 0, 'FORMULA', 11, 1, 1, '销售业绩提成'),
-- 其他收入
('OTHER_INCOME', '其他收入', 'EARNING', '其他', 0, 0, 0, 'FIXED', 12, 1, 1, '其他额外收入'),

-- ========== 扣款项 ==========
-- 社保个人部分
('SOCIAL_SECURITY', '社保(个人)', 'DEDUCTION', '社保公积金', 1, 1, 0, 'FORMULA', 20, 1, 1, '养老保险8%+医疗保险2%+失业保险0.5%'),
-- 公积金个人部分
('HOUSING_FUND', '公积金(个人)', 'DEDUCTION', '社保公积金', 1, 1, 0, 'FORMULA', 21, 1, 1, '住房公积金个人缴存比例'),
-- 请假扣款
('LEAVE_DEDUCTION', '请假扣款', 'DEDUCTION', '扣款', 0, 0, 0, 'FORMULA', 22, 1, 1, '事假等无薪假期扣款'),
-- 迟到扣款
('LATE_DEDUCTION', '迟到扣款', 'DEDUCTION', '扣款', 0, 0, 0, 'FORMULA', 23, 1, 1, '迟到超过规定时间的扣款'),
-- 其他扣款
('OTHER_DEDUCTION', '其他扣款', 'DEDUCTION', '扣款', 0, 0, 0, 'FIXED', 24, 1, 1, '其他扣款项'),

-- ========== 税项 ==========
-- 个人所得税
('PERSONAL_TAX', '个人所得税', 'TAX', '税务', 1, 1, 0, 'FORMULA', 30, 1, 1, '按个人所得税法规定计算'),
-- 年终奖计税
('YEAR_END_TAX', '年终奖计税', 'TAX', '税务', 0, 0, 0, 'FORMULA', 31, 0, 1, '年终奖单独计税部分');