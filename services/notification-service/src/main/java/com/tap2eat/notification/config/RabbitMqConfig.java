package com.tap2eat.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    @Bean
    public TopicExchange ordersExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.getExchangeName(), true, false);
    }

    @Bean
    public Queue ordersNotificationQueue(RabbitMqProperties properties) {
        return new Queue(properties.getQueueName(), true);
    }

    @Bean
    public Binding ordersNotificationBinding(
            Queue ordersNotificationQueue,
            TopicExchange ordersExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(ordersNotificationQueue)
                .to(ordersExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper orderEventObjectMapper() {
        return new ObjectMapper();
    }
}
