package com.tap2eat.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tap2eat.rabbitmq")
public class RabbitMqProperties {

    @Valid
    private DestinationProperties orders = new DestinationProperties(
            "tap2eat.orders",
            "tap2eat.notifications.orders",
            "order.#"
    );

    @Valid
    private DestinationProperties payments = new DestinationProperties(
            "tap2eat.payments",
            "tap2eat.notifications.payments",
            "payment.#"
    );

    public DestinationProperties getOrders() {
        return orders;
    }

    public void setOrders(DestinationProperties orders) {
        this.orders = orders;
    }

    public DestinationProperties getPayments() {
        return payments;
    }

    public void setPayments(DestinationProperties payments) {
        this.payments = payments;
    }

    public String getExchangeName() {
        return orders.getExchangeName();
    }

    public void setExchangeName(String exchangeName) {
        orders.setExchangeName(exchangeName);
    }

    public String getQueueName() {
        return orders.getQueueName();
    }

    public void setQueueName(String queueName) {
        orders.setQueueName(queueName);
    }

    public String getRoutingKey() {
        return orders.getRoutingKey();
    }

    public void setRoutingKey(String routingKey) {
        orders.setRoutingKey(routingKey);
    }

    public static class DestinationProperties {

        @NotBlank
        private String exchangeName;

        @NotBlank
        private String queueName;

        @NotBlank
        private String routingKey;

        public DestinationProperties() {
        }

        public DestinationProperties(String exchangeName, String queueName, String routingKey) {
            this.exchangeName = exchangeName;
            this.queueName = queueName;
            this.routingKey = routingKey;
        }

        public String getExchangeName() {
            return exchangeName;
        }

        public void setExchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
        }

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }
}
