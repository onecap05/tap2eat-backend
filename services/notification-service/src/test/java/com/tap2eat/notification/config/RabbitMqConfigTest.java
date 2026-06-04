package com.tap2eat.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void shouldCreatePaymentExchangeWithConfiguredName() {
        RabbitMqProperties properties = properties("custom.orders", "custom.notifications", "order.#");
        properties.getPayments().setExchangeName("custom.payments");

        TopicExchange exchange = config.paymentsExchange(properties);

        assertEquals("custom.payments", exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    void shouldCreatePaymentQueueWithConfiguredName() {
        RabbitMqProperties properties = properties("custom.orders", "custom.notifications", "order.#");
        properties.getPayments().setQueueName("custom.payment.notifications");

        Queue queue = config.paymentsNotificationQueue(properties);

        assertEquals("custom.payment.notifications", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void shouldCreatePaymentBindingWithRoutingKeyPaymentHash() {
        RabbitMqProperties properties = properties("tap2eat.orders", "tap2eat.notifications.orders", "order.#");
        properties.getPayments().setExchangeName("tap2eat.payments");
        properties.getPayments().setQueueName("tap2eat.notifications.payments");
        properties.getPayments().setRoutingKey("payment.#");
        TopicExchange exchange = config.paymentsExchange(properties);
        Queue queue = config.paymentsNotificationQueue(properties);

        Binding binding = config.paymentsNotificationBinding(queue, exchange, properties);

        assertEquals("tap2eat.notifications.payments", binding.getDestination());
        assertEquals("tap2eat.payments", binding.getExchange());
        assertEquals("payment.#", binding.getRoutingKey());
    }

    @Test
    void shouldExposeDefaultDestinations() {
        RabbitMqProperties properties = new RabbitMqProperties();

        assertEquals("tap2eat.orders", properties.getOrders().getExchangeName());
        assertEquals("tap2eat.notifications.orders", properties.getOrders().getQueueName());
        assertEquals("order.#", properties.getOrders().getRoutingKey());
        assertEquals("tap2eat.payments", properties.getPayments().getExchangeName());
        assertEquals("tap2eat.notifications.payments", properties.getPayments().getQueueName());
        assertEquals("payment.#", properties.getPayments().getRoutingKey());
    }

    @Test
    void shouldSupportLegacyOrderAccessors() {
        RabbitMqProperties properties = new RabbitMqProperties();

        properties.setExchangeName("legacy.orders");
        properties.setQueueName("legacy.notifications");
        properties.setRoutingKey("legacy.#");

        assertEquals("legacy.orders", properties.getExchangeName());
        assertEquals("legacy.notifications", properties.getQueueName());
        assertEquals("legacy.#", properties.getRoutingKey());
        assertEquals("legacy.orders", properties.getOrders().getExchangeName());
    }

    @Test
    void shouldReplaceNestedDestinations() {
        RabbitMqProperties properties = new RabbitMqProperties();
        RabbitMqProperties.DestinationProperties orders = new RabbitMqProperties.DestinationProperties();
        orders.setExchangeName("orders.exchange");
        orders.setQueueName("orders.queue");
        orders.setRoutingKey("orders.routing");
        RabbitMqProperties.DestinationProperties payments = new RabbitMqProperties.DestinationProperties(
                "payments.exchange",
                "payments.queue",
                "payments.routing"
        );

        properties.setOrders(orders);
        properties.setPayments(payments);

        assertEquals("orders.exchange", properties.getOrders().getExchangeName());
        assertEquals("orders.queue", properties.getOrders().getQueueName());
        assertEquals("orders.routing", properties.getOrders().getRoutingKey());
        assertEquals("payments.exchange", properties.getPayments().getExchangeName());
        assertEquals("payments.queue", properties.getPayments().getQueueName());
        assertEquals("payments.routing", properties.getPayments().getRoutingKey());
    }

    @Test
    void shouldCreateObjectMapperWhenMissing() {
        ObjectMapper objectMapper = config.orderEventObjectMapper();

        assertEquals(ObjectMapper.class, objectMapper.getClass());
    }

    private RabbitMqProperties properties(String exchangeName, String queueName, String routingKey) {
        RabbitMqProperties properties = new RabbitMqProperties();
        properties.setExchangeName(exchangeName);
        properties.setQueueName(queueName);
        properties.setRoutingKey(routingKey);
        return properties;
    }
}
