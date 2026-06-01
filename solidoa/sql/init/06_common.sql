-- oa_file 文件表
CREATE TABLE IF NOT EXISTS oa_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL COMMENT '文件名(UUID)',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    bucket VARCHAR(50) DEFAULT 'common' COMMENT '存储桶',
    business_type VARCHAR(20) COMMENT '业务类型:LEAVE,EXPENSE,ATTENDANCE',
    business_id BIGINT COMMENT '业务ID',
    uploader_id BIGINT NOT NULL COMMENT '上传人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_dingtalk_sync_log 钉钉同步日志表
CREATE TABLE IF NOT EXISTS oa_dingtalk_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(20) NOT NULL COMMENT '同步类型:USER,DEPT,ATTENDANCE',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    success_count INT DEFAULT 0 COMMENT '成功数量',
    fail_count INT DEFAULT 0 COMMENT '失败数量',
    error_msg TEXT COMMENT '错误信息',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态:RUNNING,SUCCESS,FAILED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oa_dingtalk_config 钉钉配置表
CREATE TABLE IF NOT EXISTS oa_dingtalk_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_key VARCHAR(100) NOT NULL COMMENT '应用Key',
    app_secret VARCHAR(200) NOT NULL COMMENT '应用Secret',
    agent_id VARCHAR(50) COMMENT 'AgentId',
    corp_id VARCHAR(100) COMMENT '企业ID',
    callback_url VARCHAR(200) COMMENT '回调地址',
    callback_token VARCHAR(100) COMMENT '回调Token',
    callback_aes_key VARCHAR(200) COMMENT '回调AES密钥',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;