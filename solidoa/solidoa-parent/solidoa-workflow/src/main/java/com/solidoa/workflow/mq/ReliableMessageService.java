package com.solidoa.workflow.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.workflow.config.RabbitMQConfig;
import com.solidoa.workflow.entity.MessageOutbox;
import com.solidoa.workflow.mapper.MessageOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息发送服务
 * - 记录消息到本地消息表
 * - 尝试发送 MQ 消息
 * - 提供补偿机制
 */
@Service
@Slf4j
public class ReliableMessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageOutboxMapper outboxMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送消息（带本地消息表记录）
     * 无论成功失败均写入 MessageOutbox（成功标记 SENT/补偿标记 PENDING）
     */
    public void sendMessage(String routingKey, Object message, String messageId) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKFLOW_EXCHANGE,
                routingKey,
                message
            );
            // 发送成功也记录状态为 SENT
            saveToOutbox(messageId, routingKey, message, null, "SENT");
            log.info("MQ消息发送成功: messageId={}, routingKey={}", messageId, routingKey);
        } catch (Exception e) {
            log.error("MQ消息发送失败，记录到本地消息表待补偿: messageId={}, error={}",
                messageId, e.getMessage());
            // 发送失败记录状态为 PENDING，等待补偿
            saveToOutbox(messageId, routingKey, message, e.getMessage(), "PENDING");
        }
    }

    /**
     * 保存到本地消息表
     * @param status 消息状态：SENT（已发送成功）/ PENDING（待补偿）
     */
    protected void saveToOutbox(String messageId, String routingKey, Object message, String error, String status) {
        try {
            MessageOutbox outbox = new MessageOutbox();
            outbox.setMessageId(messageId);
            outbox.setMessageType("WORKFLOW");
            outbox.setBusinessType(routingKey);
            outbox.setMessageContent(objectMapper.writeValueAsString(message));
            outbox.setStatus(status);
            outbox.setRetryCount(0);
            outbox.setMaxRetryCount(3);
            outbox.setLastError(error);
            outbox.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
            outboxMapper.insert(outbox);
            log.info("消息已保存到本地消息表: messageId={}, status={}", messageId, status);
        } catch (Exception ex) {
            log.error("保存本地消息表失败: messageId={}, error={}", messageId, ex.getMessage(), ex);
        }
    }
}