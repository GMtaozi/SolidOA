package com.solidoa.collaboration.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "solidoa.exchange";
    public static final String QUEUE_APPROVAL = "queue.approval.notify";
    public static final String QUEUE_MESSAGE = "queue.message.push";
    public static final String ROUTING_KEY_APPROVAL = "approval.notify.*";
    public static final String ROUTING_KEY_MESSAGE = "message.push.*";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue approvalQueue() {
        return QueueBuilder.durable(QUEUE_APPROVAL)
                .withArgument("x-dead-letter-exchange", "solidoa.dlx")
                .withArgument("x-dead-letter-routing-key", "approval.notify.dead")
                .build();
    }

    @Bean
    public Queue messageQueue() {
        return QueueBuilder.durable(QUEUE_MESSAGE).build();
    }

    @Bean
    public Binding approvalBinding(Queue approvalQueue, TopicExchange exchange) {
        return BindingBuilder.bind(approvalQueue).to(exchange).with(ROUTING_KEY_APPROVAL);
    }

    @Bean
    public Binding messageBinding(Queue messageQueue, TopicExchange exchange) {
        return BindingBuilder.bind(messageQueue).to(exchange).with(ROUTING_KEY_MESSAGE);
    }
}