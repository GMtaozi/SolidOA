package com.solidoa.workflow.config;

import com.solidoa.workflow.entity.MessageOutbox;
import com.solidoa.workflow.mapper.MessageOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String WORKFLOW_EXCHANGE = "workflow.exchange";
    public static final String WORKFLOW_DLX_EXCHANGE = "workflow.exchange.dlx";
    public static final String WORKFLOW_APPROVAL_QUEUE = "workflow.approval.queue";
    public static final String WORKFLOW_REMINDER_QUEUE = "workflow.reminder.queue";
    public static final String WORKFLOW_DEAD_QUEUE = "workflow.dead.queue";
    public static final String WORKFLOW_APPROVAL_ROUTING_KEY = "workflow.approval";
    public static final String WORKFLOW_REMINDER_ROUTING_KEY = "workflow.reminder";

    @Bean
    public TopicExchange workflowExchange() {
        return new TopicExchange(WORKFLOW_EXCHANGE);
    }

    @Bean
    public TopicExchange workflowDlxExchange() {
        return new TopicExchange(WORKFLOW_DLX_EXCHANGE);
    }

    @Bean
    public Queue approvalQueue() {
        return QueueBuilder.durable(WORKFLOW_APPROVAL_QUEUE)
                .withArgument("x-dead-letter-exchange", WORKFLOW_DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue reminderQueue() {
        return QueueBuilder.durable(WORKFLOW_REMINDER_QUEUE)
                .withArgument("x-dead-letter-exchange", WORKFLOW_DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(WORKFLOW_DEAD_QUEUE).build();
    }

    @Bean
    public Binding approvalBinding(@Qualifier("approvalQueue") Queue approvalQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(approvalQueue).to(workflowExchange).with(WORKFLOW_APPROVAL_ROUTING_KEY);
    }

    @Bean
    public Binding reminderBinding(@Qualifier("reminderQueue") Queue reminderQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(reminderQueue).to(workflowExchange).with(WORKFLOW_REMINDER_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, TopicExchange workflowDlxExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(workflowDlxExchange).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageOutboxMapper messageOutboxMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败: correlationData={}, cause={}", correlationData, cause);
                // 消息发送失败时，保存到 MessageOutbox 表进行补偿
                if (correlationData != null) {
                    try {
                        LocalDateTime now = LocalDateTime.now();
                        MessageOutbox outbox = new MessageOutbox();
                        outbox.setMessageId(correlationData.getId());
                        outbox.setMessageType("APPROVAL");
                        outbox.setStatus("PENDING");
                        outbox.setRetryCount(0);
                        outbox.setMaxRetryCount(5);
                        outbox.setNextRetryTime(now.plusMinutes(5));
                        outbox.setLastError(cause);
                        outbox.setCreateTime(now);
                        // 使用 insertIgnore 防止并发重复插入（依赖 message_id UNIQUE 约束）
                        int affected = messageOutboxMapper.insertIgnore(
                                outbox.getMessageId(), outbox.getMessageType(), null, null,
                                null, outbox.getMaxRetryCount(), outbox.getNextRetryTime(),
                                outbox.getLastError(), outbox.getCreateTime());
                        if (affected > 0) {
                            log.info("消息已保存到 Outbox 进行补偿: messageId={}", correlationData.getId());
                        } else {
                            log.warn("消息已存在 Outbox 中，跳过重复插入: messageId={}", correlationData.getId());
                        }
                    } catch (Exception e) {
                        log.error("保存消息到 Outbox 失败: messageId={}, error={}",
                                correlationData.getId(), e.getMessage());
                    }
                }
            }
        });
        template.setReturnsCallback(returned -> {
            log.error("消息返回: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
        });
        return template;
    }
}