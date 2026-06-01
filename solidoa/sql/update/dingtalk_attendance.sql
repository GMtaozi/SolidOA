-- 钉钉考勤集成数据库脚本
-- 包含：考勤同步日志表、钉钉用户绑定表、考勤扩展表、加班扩展表

-- =============================================
-- 1. 考勤同步日志表
-- =============================================
CREATE TABLE IF NOT EXISTS oa_attendance_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(20) NOT NULL COMMENT '同步类型:CLOCK打卡/OVERTIME加班/APPROVAL审批',
    sync_start_time DATETIME NOT NULL COMMENT '同步开始时间',
    sync_end_time DATETIME COMMENT '同步结束时间',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态:SUCCESS/FAILED/RUNNING',
    records_count INT DEFAULT 0 COMMENT '同步记录数',
    error_message TEXT COMMENT '错误信息',
    sync_params VARCHAR(500) COMMENT '同步参数(JSON)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sync_type (sync_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤同步日志表';

-- =============================================
-- 2. 钉钉用户绑定表
-- =============================================
CREATE TABLE IF NOT EXISTS oa_dingtalk_user_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT 'SolidOA用户ID',
    dingtalk_user_id VARCHAR(64) NOT NULL COMMENT '钉钉用户ID',
    dingtalk_union_id VARCHAR(64) COMMENT '钉钉UnionID',
    department_id VARCHAR(64) COMMENT '钉钉部门ID',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    unbind_time DATETIME COMMENT '解绑时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_dingtalk_user_id (dingtalk_user_id),
    INDEX idx_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钉钉员工绑定表';

-- =============================================
-- 3. 考勤记录扩展表（钉钉关联）
-- =============================================
CREATE TABLE IF NOT EXISTS oa_attendance_record_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '考勤记录ID(关联oa_attendance_record)',
    dingtalk_record_id VARCHAR(64) COMMENT '钉钉打卡记录ID',
    dingtalk_device_id VARCHAR(64) COMMENT '钉钉设备ID',
    location_result VARCHAR(20) COMMENT '定位结果:LOCATED定位成功/OUT_OF_RANGE超出范围/NORMAL正常',
    work_distance DECIMAL(10,2) COMMENT '打卡距离(米)',
    base_check_time DATETIME COMMENT '基准打卡时间',
    outside_range BOOLEAN DEFAULT FALSE COMMENT '是否超出打卡范围',
    position_longitude VARCHAR(20) COMMENT '打卡经度',
    position_latitude VARCHAR(20) COMMENT '打卡纬度',
    position_accuracy DECIMAL(10,2) COMMENT '定位精度(米)',
    dingtalk_source VARCHAR(20) COMMENT '数据来源:APP/PC/BEHALF代打卡',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '是否有效记录',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_id (record_id),
    INDEX idx_dingtalk_record_id (dingtalk_record_id),
    INDEX idx_dingtalk_source (dingtalk_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录钉钉扩展表';

-- =============================================
-- 4. 加班记录扩展表
-- =============================================
CREATE TABLE IF NOT EXISTS oa_overtime_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    overtime_id BIGINT COMMENT '加班记录ID(关联oa_overtime或oa_attendance)',
    dingtalk_overtime_id VARCHAR(64) COMMENT '钉钉加班记录ID',
    dingtalk_flow_id VARCHAR(64) COMMENT '钉钉审批流程ID',
    dingtalk_approve_result VARCHAR(20) COMMENT '钉钉审批结果',
    work_group_id VARCHAR(64) COMMENT '加班工作日组ID',
    work_group_name VARCHAR(100) COMMENT '加班工作日组名称',
    duration_hours DECIMAL(5,2) COMMENT '加班时长(小时)',
    overtime_type VARCHAR(20) COMMENT '加班类型:WD工作日/WE休息日/HOLIDAY节假日',
    converter_unit INT COMMENT '调休单位(小时)',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_overtime_id (overtime_id),
    INDEX idx_dingtalk_overtime_id (dingtalk_overtime_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录钉钉扩展表';

-- =============================================
-- 5. 考勤异常表
-- =============================================
CREATE TABLE IF NOT EXISTS oa_attendance_anomaly (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    anomaly_type VARCHAR(20) NOT NULL COMMENT '异常类型:LATE迟到/EARLY_LEAVE早退/ABSENT缺卡/TRUANCY旷工',
    anomaly_date DATE NOT NULL COMMENT '异常日期',
    description VARCHAR(200) COMMENT '异常描述',
    severity INT DEFAULT 1 COMMENT '严重程度:1普通/2重要/3紧急',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态:PENDING待处理/PROCESSED已处理/IGNORED已忽略',
    process_type VARCHAR(20) COMMENT '处理方式:SUPPLY补卡/LEAVE请假/OVERTIME转加班/IGNORE忽略',
    process_reason VARCHAR(200) COMMENT '处理原因',
    process_time DATETIME COMMENT '处理时间',
    process_user_id BIGINT COMMENT '处理人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_anomaly_date (anomaly_date),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤异常表';

-- =============================================
-- 6. 初始化钉钉用户绑定数据（示例）
-- =============================================
-- INSERT INTO oa_dingtalk_user_bind (user_id, dingtalk_user_id, dingtalk_union_id, department_id, is_active)
-- VALUES (1, 'dingtalk_user_001', 'union_001', 'dept_001', TRUE);

-- =============================================
-- 7. 初始化考勤配置数据（示例）
-- =============================================
-- INSERT INTO oa_attendance_config (id, user_id, work_start_time, work_end_time, late_minutes, early_minutes, is_active)
-- VALUES (1, NULL, '09:00:00', '18:00:00', 5, 5, TRUE);  -- 全局配置

COMMIT;