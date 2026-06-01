-- ==========================================
-- SolidOA 考勤与工资相关表结构
-- 版本：V1.3
-- 日期：2026-05-26
-- 说明：包含考勤规则、加班、出差、假期、工资等19张表
-- ==========================================

-- 1. oa_attendance_rule - 考勤规则配置表
CREATE TABLE oa_attendance_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    work_start_time TIME NOT NULL COMMENT '上班时间',
    work_end_time TIME NOT NULL COMMENT '下班时间',
    morning_start TIME COMMENT '上午签到开始时间',
    morning_end TIME COMMENT '上午签到结束时间',
    afternoon_start TIME COMMENT '下午签到开始时间',
    afternoon_end TIME COMMENT '下午签到结束时间',
    late_minutes INT DEFAULT 0 COMMENT '多少分钟后算迟到(分钟)',
    early_leave_minutes INT DEFAULT 0 COMMENT '多少分钟后算早退(分钟)',
    min_work_hours DECIMAL(4,1) DEFAULT 8.0 COMMENT '每日最低工时要求',
    require_location TINYINT DEFAULT 1 COMMENT '是否需要定位打卡:0否,1是',
    allow_remote TINYINT DEFAULT 0 COMMENT '是否允许远程打卡:0否,1是',
    max_remote_distance INT DEFAULT 500 COMMENT '远程打卡最大距离(米)',
    repair_card_limit INT DEFAULT 3 COMMENT '每月补卡次数限制',
    repair_card_require_approval TINYINT DEFAULT 1 COMMENT '补卡是否需要审批:0否,1是',
    overtime_threshold_minutes INT DEFAULT 30 COMMENT '加班认定阈值(分钟)',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认规则:0否,1是',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_code (rule_code),
    INDEX idx_is_default (is_default),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则配置表';

-- 2. oa_attendance_shift - 班次配置表
CREATE TABLE oa_attendance_shift (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_name VARCHAR(100) NOT NULL COMMENT '班次名称',
    shift_code VARCHAR(50) NOT NULL UNIQUE COMMENT '班次编码',
    shift_type VARCHAR(20) NOT NULL COMMENT '班次类型:NORMAL正常班,NIGHT夜班,FLEX弹性,SPLIT分班',
    work_start_time TIME NOT NULL COMMENT '上班时间',
    work_end_time TIME NOT NULL COMMENT '下班时间',
    break_start_time TIME COMMENT '午休开始时间',
    break_end_time TIME COMMENT '午休结束时间',
    late_tolerance_minutes INT DEFAULT 0 COMMENT '迟到容许分钟数',
    early_leave_tolerance_minutes INT DEFAULT 0 COMMENT '早退容许分钟数',
    work_hours DECIMAL(4,1) NOT NULL COMMENT '工作时长(小时)',
    is_night_shift TINYINT DEFAULT 0 COMMENT '是否夜班:0否,1是',
    night_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '夜班补贴金额',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shift_code (shift_code),
    INDEX idx_shift_type (shift_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班次配置表';

-- 3. oa_attendance_group - 考勤组表
CREATE TABLE oa_attendance_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL COMMENT '考勤组名称',
    group_code VARCHAR(50) NOT NULL UNIQUE COMMENT '考勤组编码',
    attendance_type VARCHAR(20) NOT NULL COMMENT '考勤方式:FIXED固定班次,FLEX弹性,FREE自由打卡',
    rule_id BIGINT COMMENT '关联的考勤规则ID',
    shift_id BIGINT COMMENT '默认班次ID',
    work_days VARCHAR(50) DEFAULT '1,2,3,4,5' COMMENT '工作日(逗号分隔,1-7代表周一到周日)',
    check_in_start TIME COMMENT '签到开始时间(相对)',
    check_in_end TIME COMMENT '签到结束时间(相对)',
    check_out_start TIME COMMENT '签到开始时间(相对)',
    check_out_end TIME COMMENT '签到结束时间(相对)',
    location_required TINYINT DEFAULT 1 COMMENT '是否需要地点:0否,1是',
    location_name VARCHAR(200) COMMENT '打卡地点名称',
    longitude DECIMAL(10,6) COMMENT '打卡地点经度',
    latitude DECIMAL(10,6) COMMENT '打卡地点纬度',
    location_radius INT DEFAULT 200 COMMENT '打卡范围半径(米)',
    member_count INT DEFAULT 0 COMMENT '成员数量',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group_code (group_code),
    INDEX idx_rule_id (rule_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

-- 4. oa_attendance_holiday - 节假日配置表
CREATE TABLE oa_attendance_holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    holiday_name VARCHAR(100) NOT NULL COMMENT '节假日名称',
    holiday_date DATE NOT NULL COMMENT '节假日日期',
    holiday_type VARCHAR(20) NOT NULL COMMENT '节假日类型:FESTIVAL法定节假日,EXTRA调休工作日,PRIVATE私人工日',
    is_work_day TINYINT DEFAULT 0 COMMENT '是否需要上班:0休息,1上班',
    year VARCHAR(4) NOT NULL COMMENT '年份',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_holiday_date (holiday_date),
    INDEX idx_year (year),
    INDEX idx_holiday_type (holiday_type),
    INDEX idx_is_work_day (is_work_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日配置表';

-- 注意：oa_overtime 和 oa_overtime_break 表定义在 sql/init/08_overtime.sql 中，此处不重复定义

-- 7. oa_repair_card - 补卡申请表
CREATE TABLE oa_repair_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repair_no VARCHAR(32) NOT NULL UNIQUE COMMENT '补卡单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    repair_date DATE NOT NULL COMMENT '补卡日期',
    repair_type VARCHAR(20) NOT NULL COMMENT '补卡类型:SIGN_IN签到,SIGN_OUT签退,BOTH两者都需要',
    repair_time DATETIME NOT NULL COMMENT '补卡时间',
    check_type VARCHAR(20) COMMENT '补卡打卡类型:SIGN_IN签到,SIGN_OUT签退',
    reason VARCHAR(500) NOT NULL COMMENT '补卡原因',
    attachments VARCHAR(500) COMMENT '证明材料(逗号分隔)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待审批,APPROVED已同意,REJECTED已拒绝,CANCELLED已撤回',
    approver_id BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_comment VARCHAR(500) COMMENT '审批意见',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_repair_no (repair_no),
    INDEX idx_user_id (user_id),
    INDEX idx_repair_date (repair_date),
    INDEX idx_status (status),
    INDEX idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';

-- 8. oa_repair_card_statistics - 补卡次数统计表
CREATE TABLE oa_repair_card_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT COMMENT '部门ID',
    year_month VARCHAR(7) NOT NULL COMMENT '统计月份',
    total_count INT DEFAULT 0 COMMENT '本月申请次数',
    approved_count INT DEFAULT 0 COMMENT '已审批通过次数',
    rejected_count INT DEFAULT 0 COMMENT '已拒绝次数',
    pending_count INT DEFAULT 0 COMMENT '待审批次数',
    limit_count INT DEFAULT 3 COMMENT '本月限制次数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, year_month),
    INDEX idx_dept_month (dept_id, year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡次数统计表';

-- 9. oa_business_trip - 出差申请表
CREATE TABLE oa_business_trip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_no VARCHAR(32) NOT NULL UNIQUE COMMENT '出差单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    start_date DATE NOT NULL COMMENT '出差开始日期',
    end_date DATE NOT NULL COMMENT '出差结束日期',
    total_days INT NOT NULL COMMENT '出差总天数',
    destination VARCHAR(200) NOT NULL COMMENT '出差目的地',
    trip_purpose VARCHAR(500) NOT NULL COMMENT '出差目的',
    trip_type VARCHAR(20) DEFAULT 'DOMESTIC' COMMENT '出差类型:DOMESTIC国内,OVERSEAS境外',
    estimated_cost DECIMAL(10,2) COMMENT '预计费用',
    actual_cost DECIMAL(10,2) COMMENT '实际费用',
    attachments VARCHAR(500) COMMENT '附件(逗号分隔)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待审批,APPROVED已同意,REJECTED已拒绝,COMPLETED已完成,CANCELLED已撤回',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trip_no (trip_no),
    INDEX idx_user_id (user_id),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_status (status),
    INDEX idx_destination (destination(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差申请表';

-- 10. oa_business_trip_subsidy - 出差补贴记录表
CREATE TABLE oa_business_trip_subsidy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_id BIGINT NOT NULL COMMENT '出差单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subsidy_date DATE NOT NULL COMMENT '补贴日期',
    city_level VARCHAR(20) DEFAULT 'STANDARD' COMMENT '城市等级:STANDARD标准,ABOVE一线城市,HIGH二线城市',
    daily_subsidy DECIMAL(10,2) NOT NULL COMMENT '每日补贴金额',
    meal_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '餐费补贴',
    accommodation_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '住宿补贴',
    transportation_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '交通补贴',
    other_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '其他补贴',
    total_subsidy DECIMAL(10,2) NOT NULL COMMENT '合计补贴',
    receipt_count INT DEFAULT 0 COMMENT '发票数量',
    receipt_amount DECIMAL(10,2) DEFAULT 0 COMMENT '发票金额',
    remark VARCHAR(500) COMMENT '备注',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待确认,CONFIRMED已确认,SETTLED已结算',
    settle_time DATETIME COMMENT '结算时间',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trip_id (trip_id),
    INDEX idx_user_id (user_id),
    INDEX idx_subsidy_date (subsidy_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差补贴记录表';

-- 11. oa_leave_type - 假期类型表
CREATE TABLE oa_leave_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '假期类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '假期类型名称',
    type_name_en VARCHAR(50) COMMENT '英文名称',
    category VARCHAR(20) NOT NULL COMMENT '假期类别:SICK病假,PERSONAL事假,ANNUAL年假,SPECIAL特殊假期',
    paid_type VARCHAR(20) NOT NULL COMMENT '是否带薪:FULL_PAID全薪,HALF_PAID半薪,UNPAID无薪',
    default_days INT DEFAULT 0 COMMENT '默认天数(每年)',
    max_days INT COMMENT '最大天数限制',
    min_days INT DEFAULT 1 COMMENT '最小请假天数',
    max_days_once INT COMMENT '单次最大天数',
    require_attachment TINYINT DEFAULT 0 COMMENT '是否需要附件:0否,1是',
    require_proof_days INT DEFAULT 0 COMMENT '需要证明的天数阈值(超过此天数需要提供证明)',
    approval_required TINYINT DEFAULT 1 COMMENT '是否需要审批:0否,1是',
    color VARCHAR(20) DEFAULT '#1890FF' COMMENT '显示颜色',
    icon VARCHAR(50) COMMENT '显示图标',
    sort INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_code (type_code),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期类型表';

-- 12. oa_leave_balance - 员工假期余额表
CREATE TABLE oa_leave_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    leave_type_id BIGINT NOT NULL COMMENT '假期类型ID',
    year VARCHAR(4) NOT NULL COMMENT '年份',
    total_days DECIMAL(5,1) DEFAULT 0 COMMENT '总天数',
    used_days DECIMAL(5,1) DEFAULT 0 COMMENT '已使用天数',
    pending_days DECIMAL(5,1) DEFAULT 0 COMMENT '待审批天数',
    remaining_days DECIMAL(5,1) DEFAULT 0 COMMENT '剩余天数',
    carry_over_days DECIMAL(5,1) DEFAULT 0 COMMENT '结转天数',
    expire_days DECIMAL(5,1) DEFAULT 0 COMMENT '过期天数',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    last_adjust_date DATE COMMENT '最后调整日期',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type_year (user_id, leave_type_id, year),
    INDEX idx_leave_type_id (leave_type_id),
    INDEX idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工假期余额表';

-- 13. oa_salary - 工资单主表
CREATE TABLE oa_salary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_no VARCHAR(32) NOT NULL UNIQUE COMMENT '工资单号',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    dept_id BIGINT COMMENT '部门ID',
    salary_month VARCHAR(7) NOT NULL COMMENT '工资月份',
    basic_salary DECIMAL(10,2) DEFAULT 0 COMMENT '基本工资',
    position_salary DECIMAL(10,2) DEFAULT 0 COMMENT '岗位工资',
    performance_salary DECIMAL(10,2) DEFAULT 0 COMMENT '绩效工资',
    overtime_pay DECIMAL(10,2) DEFAULT 0 COMMENT '加班费',
    bonus DECIMAL(10,2) DEFAULT 0 COMMENT '奖金',
    commission DECIMAL(10,2) DEFAULT 0 COMMENT '提成',
    allowances DECIMAL(10,2) DEFAULT 0 COMMENT '补贴',
    other_income DECIMAL(10,2) DEFAULT 0 COMMENT '其他收入',
    gross_salary DECIMAL(10,2) DEFAULT 0 COMMENT '应发工资',
    social_security DECIMAL(10,2) DEFAULT 0 COMMENT '社保扣款',
    housing_fund DECIMAL(10,2) DEFAULT 0 COMMENT '公积金扣款',
    personal_tax DECIMAL(10,2) DEFAULT 0 COMMENT '个人所得税',
    other_deductions DECIMAL(10,2) DEFAULT 0 COMMENT '其他扣款',
    net_salary DECIMAL(10,2) DEFAULT 0 COMMENT '实发工资',
    bank_card_no VARCHAR(50) COMMENT '工资卡号',
    bank_name VARCHAR(100) COMMENT '开户银行',
    payment_date DATE COMMENT '发放日期',
    payment_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '发放状态:PENDING待发放,PROCESSING发放中,PAID已发放,FAILED发放失败',
    approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态:PENDING待审批,APPROVED已审批,REJECTED已拒绝',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_salary_no (salary_no),
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_payment_status (payment_status),
    INDEX idx_approval_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资单主表';

-- 14. oa_salary_item - 工资项配置表
CREATE TABLE oa_salary_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_code VARCHAR(50) NOT NULL UNIQUE COMMENT '工资项编码',
    item_name VARCHAR(100) NOT NULL COMMENT '工资项名称',
    item_type VARCHAR(20) NOT NULL COMMENT '工资项类型:EARNING收入项,DEDUCTION扣款项,TAX税项',
    category VARCHAR(50) COMMENT '所属类别(用于前端分组显示)',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统内置:0否,1是',
    is_required TINYINT DEFAULT 0 COMMENT '是否必填项:0否,1是',
    default_value DECIMAL(10,2) COMMENT '默认值',
    calculation_type VARCHAR(20) COMMENT '计算类型:FIXED固定,FORMULA公式,CONDITION条件',
    formula VARCHAR(500) COMMENT '计算公式',
    sort INT DEFAULT 0 COMMENT '排序号',
    visible_in_slip TINYINT DEFAULT 1 COMMENT '是否在工资条显示:0否,1是',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_item_code (item_code),
    INDEX idx_item_type (item_type),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资项配置表';

-- 15. oa_salary_notice - 工资条发送记录表
CREATE TABLE oa_salary_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id BIGINT NOT NULL COMMENT '工资单ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    salary_month VARCHAR(7) NOT NULL COMMENT '工资月份',
    notice_type VARCHAR(20) NOT NULL COMMENT '发送类型:EMAIL邮件,SMS短信,APP应用内通知,WECHAT微信公众号',
    recipient VARCHAR(200) COMMENT '接收人地址(邮箱/手机号)',
    send_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '发送状态:PENDING待发送,SENDING发送中,SENT已发送,FAILED发送失败',
    send_time DATETIME COMMENT '发送时间',
    read_status VARCHAR(20) DEFAULT 'UNREAD' COMMENT '阅读状态:UNREAD未读,READ已读',
    read_time DATETIME COMMENT '阅读时间',
    error_message VARCHAR(500) COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_salary_id (salary_id),
    INDEX idx_user_id (user_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_notice_type (notice_type),
    INDEX idx_send_status (send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条发送记录表';

-- 16. oa_salary_approval - 工资审批记录表
CREATE TABLE oa_salary_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id BIGINT NOT NULL COMMENT '工资单ID',
    approval_level INT DEFAULT 1 COMMENT '审批级别',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    approval_status VARCHAR(20) NOT NULL COMMENT '审批状态:APPROVED同意,REJECTED拒绝,RETURNED退回',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    approval_time DATETIME COMMENT '审批时间',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_salary_id (salary_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approval_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资审批记录表';

-- 17. oa_approval_record - 审批记录表
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型:LEAVE请假,OVERTIME加班,BUSINESS_TRIP出差,REPAIR_CARD补卡,EXPENSE报销等',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    approval_node_id BIGINT COMMENT '审批节点ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    approval_type VARCHAR(20) NOT NULL COMMENT '审批操作:APPROVE同意,REJECT拒绝,TRANSFER转交,ADD_SIGN加签,RETURN退回',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    approval_time DATETIME NOT NULL COMMENT '审批时间',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前节点:0否,1是',
    next_approver_id BIGINT COMMENT '下一审批人ID',
    next_approver_name VARCHAR(50) COMMENT '下一审批人姓名',
    cc_user_ids VARCHAR(500) COMMENT '抄送用户ID列表',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business_type (business_type),
    INDEX idx_business_id (business_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approval_time (approval_time),
    INDEX idx_is_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 18. oa_approval_node_detail - 审批节点明细表
CREATE TABLE oa_approval_node_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    node_order INT NOT NULL COMMENT '节点顺序',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    node_type VARCHAR(20) NOT NULL COMMENT '节点类型:APPROVAL审批,SPLIT分流,JOIN合并,CC抄送',
    approver_id BIGINT COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    approver_type VARCHAR(20) COMMENT '审批人类型:USER指定用户,ROLE角色,MANAGER上级,FORM_TARGET表单指定',
    approval_mode VARCHAR(20) COMMENT '审批模式:SINGLE单人审批,ANY任意一人通过,ALL所有人通过',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待处理,APPROVED已通过,REJECTED已拒绝,SKIPPED已跳过',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    is_timeout TINYINT DEFAULT 0 COMMENT '是否超时:0否,1是',
    timeout_hours INT COMMENT '超时时长(小时)',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_business_type (business_type),
    INDEX idx_business_id (business_id),
    INDEX idx_node_order (node_order),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点明细表';

-- 19. oa_approval_cc - 抄送记录表
CREATE TABLE oa_approval_cc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    cc_user_id BIGINT NOT NULL COMMENT '抄送人ID',
    cc_user_name VARCHAR(50) COMMENT '抄送人姓名',
    cc_reason VARCHAR(200) COMMENT '抄送原因',
    cc_time DATETIME NOT NULL COMMENT '抄送时间',
    read_status VARCHAR(20) DEFAULT 'UNREAD' COMMENT '阅读状态:UNREAD未读,READ已读',
    read_time DATETIME COMMENT '阅读时间',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business_type (business_type),
    INDEX idx_business_id (business_id),
    INDEX idx_cc_user_id (cc_user_id),
    INDEX idx_read_status (read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抄送记录表';