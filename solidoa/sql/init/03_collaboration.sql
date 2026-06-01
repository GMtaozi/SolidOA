-- oa_message 消息表
CREATE TABLE oa_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_no VARCHAR(32) NOT NULL UNIQUE COMMENT '消息编号',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    type VARCHAR(20) NOT NULL COMMENT '类型:SYSTEM系统,APPROVAL审批,ATTENDANCE考勤',
    priority TINYINT DEFAULT 0 COMMENT '优先级:0普通,1重要,2紧急',
    sender_id BIGINT COMMENT '发送人ID',
    sender_name VARCHAR(50) COMMENT '发送人姓名',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读:0未读,1已读',
    read_time DATETIME COMMENT '阅读时间',
    related_type VARCHAR(20) COMMENT '关联类型:LEAVE,EXPENSE',
    related_id BIGINT COMMENT '关联ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver (receiver_id, is_read, create_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_contact 通讯录表
CREATE TABLE oa_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(100) COMMENT '部门名称',
    real_name VARCHAR(50) COMMENT '姓名',
    mobile VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    position VARCHAR(50) COMMENT '职位',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_dept_id (dept_id),
    INDEX idx_real_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_schedule 日程表
CREATE TABLE oa_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '日程标题',
    content TEXT COMMENT '日程内容',
    location VARCHAR(200) COMMENT '地点',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    is_all_day TINYINT DEFAULT 0 COMMENT '是否全天',
    remind_before INT DEFAULT 15 COMMENT '提前提醒分钟数',
    remind_way VARCHAR(20) DEFAULT 'APP' COMMENT '提醒方式:APP,EMAIL,SMS',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '颜色标记',
    user_id BIGINT NOT NULL COMMENT '创建人',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态:NORMAL正常,CANCELLED已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_schedule_share 日程分享表
CREATE TABLE oa_schedule_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL COMMENT '日程ID',
    share_user_id BIGINT NOT NULL COMMENT '分享人ID',
    receive_user_id BIGINT NOT NULL COMMENT '接收人ID',
    can_edit TINYINT DEFAULT 0 COMMENT '是否可编辑',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule_id (schedule_id),
    INDEX idx_receive_user (receive_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;