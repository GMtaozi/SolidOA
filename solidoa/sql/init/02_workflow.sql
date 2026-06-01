-- oa_leave 请假申请表
CREATE TABLE oa_leave (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_no VARCHAR(32) NOT NULL UNIQUE COMMENT '请假单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型:ANNUAL病假,SICK事假,PERSONAL调休,BUSINESS出差',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    hours DECIMAL(5,1) DEFAULT 0 COMMENT '请假小时数',
    reason TEXT COMMENT '请假事由',
    attachments VARCHAR(500) COMMENT '附件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_current_approver (current_approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_expense 报销申请表
CREATE TABLE oa_expense (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    expense_no VARCHAR(32) NOT NULL UNIQUE COMMENT '报销单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    expense_type VARCHAR(50) NOT NULL COMMENT '报销类型:TRAVEL差旅费,OFFICE办公费,ENTERTAINMENT业务招待费,OTHER其他',
    amount DECIMAL(10,2) NOT NULL COMMENT '报销金额',
    reason TEXT COMMENT '报销事由',
    attachments VARCHAR(500) COMMENT '附件',
    bank_name VARCHAR(50) COMMENT '开户行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    payment_time DATETIME COMMENT '付款时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    INDEX idx_current_approver (current_approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_approval_record 审批记录表
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型:LEAVE,EXPENSE',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approve_type VARCHAR(20) NOT NULL COMMENT '审批类型:APPROVE同意,REJECT拒绝,TRANSFER转交,ADD_SIGN加签',
    comment TEXT COMMENT '审批意见',
    task_id VARCHAR(100) COMMENT 'Camunda任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id, create_time),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_transfer_record 转交记录表
CREATE TABLE oa_transfer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    from_approver_id BIGINT NOT NULL COMMENT '原审批人',
    to_approver_id BIGINT NOT NULL COMMENT '新审批人',
    reason VARCHAR(200) COMMENT '转交原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_reminder_record 催办记录表
CREATE TABLE oa_reminder_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL,
    business_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    reminder_count INT DEFAULT 0 COMMENT '催办次数',
    last_reminder_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 用印模块 ====================

-- oa_stamp 用印申请表
CREATE TABLE oa_stamp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stamp_no VARCHAR(32) NOT NULL UNIQUE COMMENT '用印单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    stamp_type VARCHAR(20) NOT NULL COMMENT '用印类型:PUBLIC公章,CONTRACT合同章,LEGAL法人章,DEPT部门章',
    document_name VARCHAR(200) NOT NULL COMMENT '文件名称',
    document_count INT DEFAULT 1 COMMENT '文件份数',
    usage VARCHAR(500) COMMENT '用印用途',
    attachments VARCHAR(500) COMMENT '附件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    stamp_time DATETIME COMMENT '用印时间',
    received_by VARCHAR(50) COMMENT '领取人',
    received_mobile VARCHAR(20) COMMENT '领取人电话',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    INDEX idx_stamp_type (stamp_type),
    INDEX idx_current_approver (current_approver_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_stamp_record 物理用印记录表
CREATE TABLE oa_stamp_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stamp_id BIGINT NOT NULL COMMENT '用印申请ID',
    stamp_time DATETIME NOT NULL COMMENT '用印时间',
    received_by VARCHAR(50) COMMENT '领取人',
    received_mobile VARCHAR(20) COMMENT '领取人电话',
    actual_count INT DEFAULT 1 COMMENT '实际用印份数',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stamp_id (stamp_id),
    INDEX idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 采购模块 ====================

-- oa_purchase 采购申请表
CREATE TABLE oa_purchase (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_no VARCHAR(32) NOT NULL UNIQUE COMMENT '采购单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    purchase_type VARCHAR(20) NOT NULL COMMENT '采购类型:OFFICE办公用品,IT IT设备,FURNITURE家具,SOFTWARE软件服务,OTHER其他',
    item_name VARCHAR(200) NOT NULL COMMENT '采购物品名称',
    quantity INT DEFAULT 1 COMMENT '数量',
    unit VARCHAR(20) COMMENT '单位',
    budget_amount DECIMAL(10,2) COMMENT '预算金额',
    supplier_name VARCHAR(100) COMMENT '供应商名称',
    supplier_contact VARCHAR(50) COMMENT '供应商联系人',
    supplier_phone VARCHAR(20) COMMENT '供应商电话',
    reason TEXT COMMENT '采购原因',
    attachments VARCHAR(500) COMMENT '附件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    delivery_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '交付状态',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    expected_delivery_date DATE COMMENT '期望交付日期',
    actual_delivery_date DATE COMMENT '实际交付日期',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    INDEX idx_purchase_type (purchase_type),
    INDEX idx_delivery_status (delivery_status),
    INDEX idx_current_approver (current_approver_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_purchase_item 采购明细表
CREATE TABLE oa_purchase_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL COMMENT '采购申请ID',
    item_name VARCHAR(200) NOT NULL COMMENT '物品名称',
    spec VARCHAR(200) COMMENT '规格型号',
    quantity INT DEFAULT 1 COMMENT '数量',
    unit VARCHAR(20) COMMENT '单位',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_price DECIMAL(10,2) COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchase_id (purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_purchase_progress 采购进度表
CREATE TABLE oa_purchase_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL COMMENT '采购申请ID',
    progress_type VARCHAR(20) NOT NULL COMMENT '进度类型:PURCHASING采购中,DELIVERED已到货,COMPLETED已完成',
    progress_desc VARCHAR(500) COMMENT '进度说明',
    progress_time DATETIME COMMENT '进度时间',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchase_id (purchase_id),
    INDEX idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 审批流程配置模块 ====================

-- oa_approval_flow_config 审批流程配置表
CREATE TABLE oa_approval_flow_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型:LEAVE,EXPENSE,STAMP,PURCHASE',
    flow_name VARCHAR(100) NOT NULL COMMENT '流程名称',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认流程',
    config JSON NOT NULL COMMENT '节点配置JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_approval_node 审批节点表（运行时实例）
CREATE TABLE oa_approval_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    node_order INT NOT NULL COMMENT '节点顺序',
    node_type VARCHAR(20) NOT NULL COMMENT '节点类型:APPROVER/CC',
    node_mode VARCHAR(20) DEFAULT 'ANY' COMMENT '审批模式:ALL会签,ANY或签',
    approver_id BIGINT COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人名称',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待处理,APPROVED已通过,REJECTED已拒绝',
    approved_time DATETIME COMMENT '审批时间',
    comment TEXT COMMENT '审批意见',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_approver (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_approval_cc 抄送记录表
CREATE TABLE oa_approval_cc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    cc_user_id BIGINT NOT NULL COMMENT '抄送人ID',
    cc_user_name VARCHAR(50) COMMENT '抄送人名称',
    notify_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '通知状态',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_cc_user (cc_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 审计日志模块 ====================

-- oa_audit_log 操作审计日志表
CREATE TABLE oa_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '用户名',
    module VARCHAR(50) COMMENT '操作模块',
    action VARCHAR(50) COMMENT '操作类型',
    business_type VARCHAR(50) COMMENT '业务类型',
    business_id BIGINT COMMENT '业务ID',
    description VARCHAR(500) COMMENT '操作描述',
    method VARCHAR(20) COMMENT '请求方法',
    request_path VARCHAR(200) COMMENT '请求路径',
    request_params TEXT COMMENT '请求参数',
    response_status INT COMMENT '响应状态',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_business (business_type, business_id),
    INDEX idx_module_action (module, action),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;