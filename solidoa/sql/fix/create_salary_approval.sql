-- 工资审批表
CREATE TABLE IF NOT EXISTS `oa_salary_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `salary_id` bigint NOT NULL COMMENT '工资单ID',
  `year_month` varchar(7) NOT NULL COMMENT '年月',
  `total_count` int DEFAULT 1 COMMENT '总人数',
  `total_amount` decimal(15,2) DEFAULT 0 COMMENT '总金额',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_year_month` (`year_month`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;