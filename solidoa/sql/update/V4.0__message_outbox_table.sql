-- ============================================================================
-- D1: Sprint 4.7 修复 - 建 oa_message_outbox 表
-- 实体已存在 (MessageOutbox.java)，但 sql/init/ 缺建表语句
-- 消息本地消息表（MQ 可靠投递模式）：审批通过后写一条到 outbox，后台轮询发送
-- ============================================================================
CREATE TABLE IF NOT EXISTS wf_message_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '消息唯一 ID（幂等用）',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型：APPROVAL/REMINDER/NOTIFICATION',
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型：LEAVE/EXPENSE/STAMP/PURCHASE',
    business_id BIGINT NOT NULL COMMENT '业务单据 ID',
    message_content TEXT NOT NULL COMMENT '消息内容（JSON 格式）',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PENDING_PENDING/SENDING/SENT/FAILED/ABANDONED',
    retry_count INT DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT DEFAULT 5 COMMENT '最大重试次数',
    last_error TEXT COMMENT '最后一次失败原因',
    next_retry_time DATETIME COMMENT '下次重试时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status, next_retry_time),
    INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息本地消息表（MQ 可靠投递）';
