package com.tap2eat.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void shouldCreateOrdersExchangeWithConfiguredName() {
        RabbitMqProperties properties = properties("custom.orders", "custom.notifications", "order.#");

        TopicExchange exchange = config.ordersExchange(properties);

        assertEquals("custom.orders", exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    void shouldCreateOrdersQueueWithConfiguredName() {
        RabbitMqProperties properties = properties("custom.orders", "custom.notifications", "order.#");

        Queue queue = config.ordersNotificationQueue(properties);

        assertEquals("custom.notifications", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void shouldCreateBindingWithRoutingKeyOrderHash() {
        RabbitMqProperties properties = properties("tap2eat.orders", "tap2eat.notifications.orders", "order.#");
        TopicExchange exchange = config.ordersExchange(properties);
        Queue queue = config.ordersNotificationQueue(properties);

        Binding binding = config.ordersNotificationBinding(queue, exchange, properties);

        assertEquals("tap2eat.notifications.orders", binding.getDestination());
        assertEquals("tap2eat.orders", binding.getExchange());
        assertEquals("order.#", binding.getRoutingKey());
    }

    private RabbitMqProperties properties(String exchangeName, String queueName, String routingKey) {
        RabbitMqProperties properties = new RabbitMqProperties();
        properties.setExchangeName(exchangeName);
        properties.setQueueName(queueName);
        properties.setRoutingKey(routingKey);
        return properties;
    }
}
