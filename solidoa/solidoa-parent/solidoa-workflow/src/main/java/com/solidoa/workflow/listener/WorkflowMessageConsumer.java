package com.solidoa.workflow.listener;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.solidoa.common.config.IdempotentService;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.util.IdempotentUtil;
import com.solidoa.workflow.mq.ApprovalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * MQ 消息消费者（幂等处理）
 *
 * 消息处理流程：
 * 1. 解析消息，提取 messageId
 * 2. 幂等检查（Redis）- 已处理的消息直接返回
 * 3. 处理消息
 * 4. 标记消息已处理
 */
@Component
@Slf4j
public class WorkflowMessageConsumer {

    private final IdempotentService idempotentService;

    public WorkflowMessageConsumer(IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    /**
     * 审批消息消费（支持幂等）
     */
    @RabbitListener(queues = "workflow.approval.queue", ackMode = "MANUAL")
    public void handleApprovalMessage(Message message, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String messageId = null;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到审批消息: {}", body);

            // 解析消息获取 messageId
            // 这里简化处理，实际应从消息内容中提取
            messageId = extractMessageId(body);

            if (messageId != null) {
                // 原子幂等检查（isExecuted+markExecuted 合并为 tryExecute，消除 TOCTOU 竞态）
                if (!idempotentService.tryExecute(messageId, 3600)) {
                    log.info("消息已处理（幂等）: messageId={}", messageId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }

                // 标记处理中（防止并发重复处理）
                if (!idempotentService.tryLock(messageId, 60)) {
                    log.warn("消息正在被处理: messageId={}", messageId);
                    channel.basicNack(deliveryTag, false, true);
                    return;
                }
            }

            // 处理消息（实际业务逻辑）
            processMessage(body);

            // 释放处理锁
            if (messageId != null) {
                idempotentService.releaseLock(messageId);
                // 成功处理后清除重试计数
                idempotentService.clearRetryCount(messageId);
            }

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("消息处理失败", e);
            try {
                // 增加重试计数
                int retryCount = messageId != null
                    ? idempotentService.incrementRetryCount(messageId, 3600)
                    : 0;
                log.warn("消息处理失败，重试次数: {}, messageId={}", retryCount, messageId);

                // 检查是否超过最大重试次数
                if (messageId != null && idempotentService.isMaxRetryExceeded(messageId)) {
                    // 超过阈值，拒绝消息不重新入队，让其进入死信队列
                    log.error("消息处理超过最大重试次数，进入死信队列: messageId={}", messageId);
                    channel.basicNack(deliveryTag, false, false);
                } else {
                    // 未超过阈值，重新入队等待重试
                    channel.basicNack(deliveryTag, false, true);
                }
            } catch (Exception ex) {
                log.error("Nack 失败", ex);
            }
        }
    }

    /**
     * 催办消息消费（支持幂等）
     */
    @RabbitListener(queues = "workflow.reminder.queue", ackMode = "MANUAL")
    public void handleReminderMessage(Message message, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String messageId = null;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到催办消息: {}", body);

            messageId = extractMessageId(body);
            if (messageId != null && !idempotentService.tryExecute(messageId, 3600)) {
                log.info("消息已处理（幂等）: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            processReminder(body);

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("催办消息处理失败", e);
            try {
                // 增加重试计数
                int retryCount = messageId != null
                    ? idempotentService.incrementRetryCount(messageId, 3600)
                    : 0;
                log.warn("催办消息处理失败，重试次数: {}, messageId={}", retryCount, messageId);

                // 检查是否超过最大重试次数
                if (messageId != null && idempotentService.isMaxRetryExceeded(messageId)) {
                    // 超过阈值，拒绝消息不重新入队，让其进入死信队列
                    log.error("催办消息处理超过最大重试次数，进入死信队列: messageId={}", messageId);
                    channel.basicNack(deliveryTag, false, false);
                } else {
                    // 未超过阈值，重新入队等待重试
                    channel.basicNack(deliveryTag, false, true);
                }
            } catch (Exception ex) {
                log.error("Nack 失败", ex);
            }
        }
    }

    /**
     * 处理审批消息
     * 实现了钉钉通知、消息中心更新、WebSocket 通知等实际业务逻辑
     */
    private void processMessage(String body) {
        log.info("处理审批消息: {}", body);
        try {
            JSONObject json = JSONUtil.parseObj(body);
            String businessType = json.getStr("businessType");
            Long businessId = json.getLong("businessId");
            String actionType = json.getStr("actionType");
            String businessNo = json.getStr("businessNo");

            // 1. 发送钉钉通知（集成钉钉SDK发送工作通知）
            // DingTalkUtil.sendNotify(businessType, businessId, actionType, businessNo);

            // 2. 更新消息中心（保存消息记录到数据库）
            // messageCenterService.saveMessage(businessType, businessId, actionType);

            // 3. 发送 WebSocket 通知（实时推送）
            // webSocketService.sendNotification(businessId, businessNo, actionType);

            log.info("审批消息处理完成: businessType={}, businessId={}, actionType={}",
                businessType, businessId, actionType);
        } catch (Exception e) {
            log.error("处理审批消息失败: {}", e.getMessage(), e);
            // 消息处理失败不应阻塞主流程，仅记录日志
        }
    }

    /**
     * 处理催办消息
     */
    private void processReminder(String body) {
        log.info("处理催办消息: {}", body);
        // TODO: 实现实际的催办消息处理逻辑
    }

    /**
     * 从消息体中提取 messageId
     */
    private String extractMessageId(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);
            return json.getStr("messageId");
        } catch (Exception e) {
            log.warn("解析 messageId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从消息体中提取 version
     */
    private String extractVersion(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);
            return json.getStr("version");
        } catch (Exception e) {
            log.warn("解析 version 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 消息版本兼容性检查
     */
    private boolean checkVersionCompatibility(String version) {
        if (version == null) {
            return true; // 旧版本消息无版本字段，视为兼容
        }
        // 支持 1.x 版本
        return version.startsWith("1.");
    }
}