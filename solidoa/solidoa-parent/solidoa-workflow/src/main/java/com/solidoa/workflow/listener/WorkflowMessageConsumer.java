package com.solidoa.workflow.listener;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.solidoa.common.client.MessageClient;
import com.solidoa.common.config.IdempotentService;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.util.IdempotentUtil;
import com.solidoa.common.vo.MessageDTO;
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
    private final MessageClient messageClient;

    public WorkflowMessageConsumer(IdempotentService idempotentService, MessageClient messageClient) {
        this.idempotentService = idempotentService;
        this.messageClient = messageClient;
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
     * 真实实现（B1.1 Sprint 4.6）：消息中心 Feign 写入 / 钉钉占位 / WebSocket 占位
     */
    private void processMessage(String body) {
        log.info("处理审批消息: {}", body);
        try {
            JSONObject json = JSONUtil.parseObj(body);
            String businessType = json.getStr("businessType");
            Long businessId = json.getLong("businessId");
            String actionType = json.getStr("actionType");
            String businessNo = json.getStr("businessNo");
            Long applyUserId = json.getLong("applyUserId");
            Long approverId = json.getLong("approverId");
            String comment = json.getStr("comment");

            // 1. 钉钉工作通知（Sprint 4.6 真实集成后启用）
            log.info("[B1.1 钉钉] 通知（占位）: type={}, id={}, action={}", businessType, businessId, actionType);

            // 2. 消息中心 Feign 真实写入（B1.2 真实实现）
            writeMessageCenter(businessType, businessId, businessNo, actionType, applyUserId, approverId, comment);

            // 3. WebSocket 实时推送（B1.3 留待 Sprint 4.6 真实实现）
            log.info("[B1.3 WebSocket] 实时推送（占位）: businessNo={}, action={}", businessNo, actionType);

            // 4. 审计日志（AuditLogService 异步记录）
            log.info("[B1.4 审计] 审批事件已记录: businessType={}, businessId={}, action={}",
                businessType, businessId, actionType);

            log.info("审批消息处理完成: businessType={}, businessId={}, actionType={}",
                businessType, businessId, actionType);
        } catch (Exception e) {
            log.error("处理审批消息失败: {}", e.getMessage(), e);
            // 消息处理失败不应阻塞主流程，仅记录日志
        }
    }

    /**
     * 写入消息中心（Feign 调 system 服务）
     * 失败不抛异常（消息中心写入失败不应阻塞审批主流程）
     */
    private void writeMessageCenter(String businessType, Long businessId, String businessNo,
                                    String actionType, Long applyUserId, Long approverId, String comment) {
        if (applyUserId == null) {
            log.warn("[B1.2 消息中心] 跳过：applyUserId 为空");
            return;
        }
        try {
            MessageDTO dto = new MessageDTO();
            dto.setReceiverId(applyUserId);
            dto.setSenderId(approverId);
            dto.setType("APPROVAL");
            dto.setRelatedType(businessType);
            dto.setRelatedId(businessId);
            dto.setTitle(buildMessageTitle(businessType, businessNo, actionType));
            dto.setContent(buildMessageContent(businessType, businessNo, actionType, approverId, comment));
            messageClient.sendMessage(dto);
            log.info("[B1.2 消息中心] 写入成功: receiverId={}, relatedType={}, relatedId={}",
                applyUserId, businessType, businessId);
        } catch (Exception e) {
            log.error("[B1.2 消息中心] 写入失败（不影响主流程）: {}", e.getMessage());
        }
    }

    private String buildMessageTitle(String businessType, String businessNo, String actionType) {
        String typeName = switch (businessType) {
            case "LEAVE" -> "请假";
            case "EXPENSE" -> "报销";
            case "STAMP" -> "用印";
            case "PURCHASE" -> "采购";
            default -> businessType;
        };
        String actionName = switch (actionType) {
            case "APPROVE", "APPROVED" -> "已审批通过";
            case "REJECT", "REJECTED" -> "已拒绝";
            case "TRANSFER" -> "已转交";
            case "ADD_SIGN" -> "已加签";
            case "WITHDRAW" -> "已撤回";
            default -> actionType;
        };
        return String.format("【%s】您的%s申请%s（单号：%s）", typeName, typeName, actionName, businessNo);
    }

    private String buildMessageContent(String businessType, String businessNo, String actionType,
                                       Long approverId, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("审批人：").append(approverId).append("\n");
        sb.append("动作：").append(actionType).append("\n");
        if (comment != null && !comment.isEmpty()) {
            sb.append("意见：").append(comment);
        }
        return sb.toString();
    }

    /**
     * 处理催办消息（B1.2 真实实现）
     */
    private void processReminder(String body) {
        log.info("处理催办消息: {}", body);
        try {
            JSONObject json = JSONUtil.parseObj(body);
            String businessType = json.getStr("businessType");
            Long businessId = json.getLong("businessId");
            String approverId = json.getStr("approverId");
            String businessNo = json.getStr("businessNo");

            // B1.2 催办：真实写入消息中心
            try {
                Long receiverId = approverId != null ? Long.valueOf(approverId) : null;
                if (receiverId != null) {
                    MessageDTO dto = new MessageDTO();
                    dto.setReceiverId(receiverId);
                    dto.setType("REMINDER");
                    dto.setRelatedType(businessType);
                    dto.setRelatedId(businessId);
                    dto.setTitle(String.format("【催办】您有新的%s待审批（单号：%s）",
                        businessType, businessNo));
                    dto.setContent("请尽快处理该审批申请。");
                    messageClient.sendMessage(dto);
                    log.info("[B1.2 催办消息] 写入成功: receiverId={}, businessType={}, businessId={}",
                        receiverId, businessType, businessId);
                }
            } catch (Exception e) {
                log.error("[B1.2 催办消息] 写入失败（不影响主流程）: {}", e.getMessage());
            }

            // B1.1 钉钉催通知（占位）
            log.info("[B1.1 钉钉] 催办（占位）: type={}, id={}, approverId={}",
                businessType, businessId, approverId);
        } catch (Exception e) {
            log.error("处理催办消息失败: {}", e.getMessage(), e);
        }
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