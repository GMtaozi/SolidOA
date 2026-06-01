-- 本地消息表（分布式事务补偿）
CREATE TABLE IF NOT EXISTS `sys_message_outbox` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `message_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '消息唯一标识',
    `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型：APPROVAL/REMINDER/NOTIFICATION',
    `business_type` VARCHAR(20) COMMENT '业务类型：LEAVE/EXPENSE/REPAIR',
    `business_id` BIGINT COMMENT '业务ID',
    `message_content` TEXT COMMENT '消息内容JSON',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENDING/SENT/FAILED/ABANDONED',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `max_retry_count` INT DEFAULT 5 COMMENT '最大重试次数',
    `last_error` TEXT COMMENT '最后错误信息',
    `next_retry_time` DATETIME COMMENT '下次重试时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status_retry` (`status`, `next_retry_time`),
    INDEX `idx_message_id` (`message_id`),
    INDEX `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表（本地消息表）';