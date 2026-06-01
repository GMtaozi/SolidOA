package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.MessageOutbox;
import com.solidoa.workflow.mq.ApprovalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    @Select("SELECT * FROM sys_message_outbox WHERE status IN ('PENDING', 'PENDING_PENDING') AND next_retry_time <= #{now} ORDER BY create_time ASC LIMIT #{limit}")
    List<MessageOutbox> selectPendingMessages(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT * FROM sys_message_outbox WHERE message_id = #{messageId} AND status = 'SENT' LIMIT 1")
    MessageOutbox selectByMessageId(@Param("messageId") String messageId);

    @Insert("INSERT IGNORE INTO sys_message_outbox (message_id, message_type, business_type, business_id, message_content, status, retry_count, max_retry_count, next_retry_time, last_error, create_time) " +
            "VALUES (#{messageId}, #{messageType}, #{businessType}, #{businessId}, #{content}, 'PENDING', 0, #{maxRetry}, #{nextRetryTime}, #{lastError}, #{createTime})")
    int insertIgnore(@Param("messageId") String messageId,
                     @Param("messageType") String messageType,
                     @Param("businessType") String businessType,
                     @Param("businessId") Long businessId,
                     @Param("content") String content,
                     @Param("maxRetry") int maxRetry,
                     @Param("nextRetryTime") LocalDateTime nextRetryTime,
                     @Param("lastError") String lastError,
                     @Param("createTime") LocalDateTime createTime);

    /**
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 原子化幂等检查 + 插入
     * 返回 1 表示新插入，返回 0 表示已存在（跳过）
     */
    @Insert("INSERT INTO sys_message_outbox (message_id, message_type, business_type, business_id, message_content, status, retry_count, max_retry_count, next_retry_time, create_time) " +
            "SELECT #{messageId}, #{messageType}, #{businessType}, #{businessId}, #{content}, 'PENDING', 0, #{maxRetry}, DATE_ADD(NOW(), INTERVAL #{retryInterval} SECOND), NOW() " +
            "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_message_outbox WHERE message_id = #{messageId})")
    int insertOrUpdateOnDuplicate(@Param("messageId") String messageId,
                                   @Param("messageType") String messageType,
                                   @Param("businessType") String businessType,
                                   @Param("businessId") Long businessId,
                                   @Param("content") String content,
                                   @Param("maxRetry") int maxRetry,
                                   @Param("retryInterval") int retryInterval);
}