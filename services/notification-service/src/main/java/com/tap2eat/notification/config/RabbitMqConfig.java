package com.tap2eat.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    @Bean
    public TopicExchange ordersExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.getOrders().getExchangeName(), true, false);
    }

    @Bean
    public Queue ordersNotificationQueue(RabbitMqProperties properties) {
        return new Queue(properties.getOrders().getQueueName(), true);
    }

    @Bean
    public Binding ordersNotificationBinding(
            @Qualifier("ordersNotificationQueue") Queue ordersNotificationQueue,
            @Qualifier("ordersExchange") TopicExchange ordersExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(ordersNotificationQueue)
                .to(ordersExchange)
                .with(properties.getOrders().getRoutingKey());
    }

    @Bean
    public TopicExchange paymentsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.getPayments().getExchangeName(), true, false);
    }

    @Bean
    public Queue paymentsNotificationQueue(RabbitMqProperties properties) {
        return new Queue(properties.getPayments().getQueueName(), true);
    }

    @Bean
    public Binding paymentsNotificationBinding(
            @Qualifier("paymentsNotificationQueue") Queue paymentsNotificationQueue,
            @Qualifier("paymentsExchange") TopicExchange paymentsExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(paymentsNotificationQueue)
                .to(paymentsExchange)
                .with(properties.getPayments().getRoutingKey());
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper orderEventObjectMapper() {
        return new ObjectMapper();
    }
}
