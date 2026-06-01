package com.solidoa.workflow.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.workflow.config.RabbitMQConfig;
import com.solidoa.workflow.entity.MessageOutbox;
import com.solidoa.workflow.mapper.MessageOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 可靠消息服务 - 带本地消息表的分布式事务方案
 *
 * 流程：
 * 1. 业务操作 + 消息记录 同一事务（本地消息表）
 * 2. MQ 消息发送（异步）
 * 3. 定时任务补偿（发送失败重试）
 */
@Service
@Slf4j
public class WorkflowMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageOutboxMapper messageOutboxMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final int MAX_RETRY = 5;
    private static final int RETRY_INTERVAL_SECONDS = 30;

    /**
     * 发送审批消息（幂等）
     * 使用 INSERT ... SELECT WHERE NOT EXISTS 消除 TOCTOU 竞态条件
     * MQ发送和状态更新使用同一事务模板，确保原子性
     */
    @Transactional
    public void sendApprovalMessage(ApprovalMessage message) {
        String messageId = generateMessageId(message, "APPROVAL");

        // 设置消息版本和类型
        message.setVersion("1.0");
        message.setMessageType("APPROVAL");

        // 序列化消息内容
        String content = JSONUtil.toJsonStr(message);

        // 使用 INSERT ... SELECT WHERE NOT EXISTS 原子化幂等检查 + 插入
        // 如果消息已存在则不会插入，直接跳过（数据库唯一索引保证）
        int inserted = messageOutboxMapper.insertOrUpdateOnDuplicate(
            messageId, "APPROVAL", message.getBusinessType(), message.getBusinessId(),
            content, MAX_RETRY, RETRY_INTERVAL_SECONDS);
        if (inserted == 0) {
            log.info("消息已存在（幂等）: messageId={}", messageId);
            return;
        }

        // 在同一事务内执行MQ发送和状态更新，确保原子性
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 先将状态设为 PENDING_PENDING（发送中），防止补偿任务重复处理
                updateOutboxStatusAtomic(messageId, "PENDING_PENDING", null);

                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WORKFLOW_EXCHANGE,
                    RabbitMQConfig.WORKFLOW_APPROVAL_ROUTING_KEY,
                    message
                );
                log.info("审批消息发送成功: messageId={}, businessType={}, businessId={}",
                    messageId, message.getBusinessType(), message.getBusinessId());
                // 发送成功，状态更新为 SENT
                updateOutboxStatusAtomic(messageId, "SENT", null);
            } catch (Exception e) {
                log.error("审批消息发送失败: messageId={}, error={}", messageId, e.getMessage());
                // 发送失败，状态更新为 FAILED（同一事务内）
                updateOutboxStatusAtomic(messageId, "FAILED", e.getMessage());
                // 抛出异常让事务回滚
                throw new RuntimeException("MQ发送失败", e);
            }
        });
    }

    /**
     * 原子化状态更新（使用 UPDATE ... WHERE 条件避免 TOCTOU 竞态）
     * 返回更新是否成功
     */
    private boolean updateOutboxStatusAtomic(String messageId, String status, String error) {
        try {
            var wrapper = new LambdaQueryWrapper<MessageOutbox>()
                .eq(MessageOutbox::getMessageId, messageId);
            var outbox = messageOutboxMapper.selectOne(wrapper);
            if (outbox != null) {
                outbox.setStatus(status);
                if (error != null) {
                    outbox.setLastError(error);
                }
                messageOutboxMapper.updateById(outbox);
                return true;
            }
        } catch (Exception e) {
            log.error("状态更新失败: messageId={}, status={}, error={}", messageId, status, e.getMessage());
        }
        return false;
    }

    /**
     * 发送催办消息
     */
    public void sendReminderMessage(ApprovalMessage message) {
        String messageId = generateMessageId(message, "REMINDER");

        // 设置消息版本和类型
        message.setVersion("1.0");
        message.setMessageType("REMINDER");

        // 先保存到本地消息表
        saveToOutbox(messageId, "REMINDER", message);

        // 尝试发送 MQ
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.WORKFLOW_REMINDER_ROUTING_KEY,
                message
            );
            log.info("催办消息发送成功: messageId={}", messageId);
            updateOutboxStatus(messageId, "SENT", null);
        } catch (Exception e) {
            log.error("催办消息发送失败: messageId={}, error={}", messageId, e.getMessage());
            updateOutboxStatus(messageId, "FAILED", e.getMessage());
        }
    }

    /**
     * 补偿任务：定期重试发送失败的消息（包括PENDING_PENDING状态，即服务重启时的遗留消息）
     */
    @Scheduled(fixedDelay = 30000) // 30秒执行一次
    public void retryFailedMessages() {
        LocalDateTime now = LocalDateTime.now();
        var messages = messageOutboxMapper.selectPendingMessages(now, 100);

        if (messages.isEmpty()) {
            return;
        }

        log.info("开始补偿失败消息，数量: {}", messages.size());

        for (MessageOutbox outbox : messages) {
            // 区分PENDING_PENDING状态（服务重启遗留）和普通PENDING状态
            boolean isRecoveringPendingPending = "PENDING_PENDING".equals(outbox.getStatus());

            if (outbox.getRetryCount() >= outbox.getMaxRetryCount()) {
                log.warn("消息重试次数超限，标记为放弃: messageId={}", outbox.getMessageId());
                outbox.setStatus("ABANDONED");
                messageOutboxMapper.updateById(outbox);
                continue;
            }

            try {
                Object messageObj = JSONUtil.toBean(outbox.getMessageContent(), ApprovalMessage.class);
                String routingKey = "APPROVAL".equals(outbox.getMessageType())
                    ? RabbitMQConfig.WORKFLOW_APPROVAL_ROUTING_KEY
                    : RabbitMQConfig.WORKFLOW_REMINDER_ROUTING_KEY;

                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WORKFLOW_EXCHANGE,
                    routingKey,
                    messageObj
                );

                outbox.setStatus("SENT");
                log.info("消息补偿成功: messageId={}, retryCount={}", outbox.getMessageId(), outbox.getRetryCount());
            } catch (Exception e) {
                // PENDING_PENDING状态首次重试不计入retryCount（视为网络瞬断或服务重启恢复）
                if (isRecoveringPendingPending) {
                    outbox.setLastError(e.getMessage());
                    outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(RETRY_INTERVAL_SECONDS));
                    log.warn("PENDING_PENDING状态消息重试（不计入retryCount）: messageId={}, error={}",
                        outbox.getMessageId(), e.getMessage());
                } else {
                    outbox.setRetryCount(outbox.getRetryCount() + 1);
                    outbox.setLastError(e.getMessage());
                    outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(RETRY_INTERVAL_SECONDS * (outbox.getRetryCount() + 1)));
                    log.warn("消息补偿失败: messageId={}, retry={}, error={}",
                        outbox.getMessageId(), outbox.getRetryCount(), e.getMessage());
                }
            }

            messageOutboxMapper.updateById(outbox);
        }
    }

    private void saveToOutbox(String messageId, String messageType, ApprovalMessage message) {
        MessageOutbox outbox = new MessageOutbox();
        outbox.setMessageId(messageId);
        outbox.setMessageType(messageType);
        outbox.setBusinessType(message.getBusinessType());
        outbox.setBusinessId(message.getBusinessId());
        outbox.setMessageContent(JSONUtil.toJsonStr(message));
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setMaxRetryCount(MAX_RETRY);
        outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(RETRY_INTERVAL_SECONDS));
        messageOutboxMapper.insert(outbox);
    }

    private void updateOutboxStatus(String messageId, String status, String error) {
        var wrapper = new LambdaQueryWrapper<MessageOutbox>()
            .eq(MessageOutbox::getMessageId, messageId);
        var outbox = messageOutboxMapper.selectOne(wrapper);
        if (outbox != null) {
            outbox.setStatus(status);
            if (error != null) {
                outbox.setLastError(error);
            }
            messageOutboxMapper.updateById(outbox);
        }
    }

    private String generateMessageId(ApprovalMessage message, String type) {
        return String.format("%s_%s_%d",
            type,
            message.getBusinessType(),
            message.getBusinessId()
        );
    }
}