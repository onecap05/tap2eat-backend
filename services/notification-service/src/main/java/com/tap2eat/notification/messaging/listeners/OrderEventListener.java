package com.tap2eat.notification.messaging.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.services.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private static final String ORDER_CREATED_EVENT_TYPE = "order.created";
    private static final String ORDER_STATUS_CHANGED_EVENT_TYPE = "order.status.changed";
    private static final String EVENT_TYPE_PROPERTY = "EventType";
    private static final String EVENT_TYPE_ALIAS_PROPERTY = "eventType";

    private final ObjectMapper objectMapper;
    private final INotificationService notificationService;

    public OrderEventListener(ObjectMapper objectMapper, INotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${tap2eat.rabbitmq.orders.queue-name:tap2eat.notifications.orders}")
    public void handleOrderEvent(String rawMessage) {
        try {
            JsonNode payload = objectMapper.readTree(rawMessage);
            String eventType = getEventType(payload);

            if (ORDER_CREATED_EVENT_TYPE.equals(eventType)) {
                OrderCreatedEvent event = objectMapper.treeToValue(payload, OrderCreatedEvent.class);
                notificationService.handleOrderCreated(event);
                return;
            }

            if (ORDER_STATUS_CHANGED_EVENT_TYPE.equals(eventType)) {
                OrderStatusChangedEvent event = objectMapper.treeToValue(payload, OrderStatusChangedEvent.class);
                notificationService.handleOrderStatusChanged(event);
                return;
            }

            log.warn("Ignoring unknown order event type: {}", eventType);
        } catch (JsonProcessingException ex) {
            log.warn("Ignoring invalid order event JSON payload: {}", ex.getOriginalMessage());
        }
    }

    private String getEventType(JsonNode payload) {
        JsonNode eventTypeNode = payload.get(EVENT_TYPE_PROPERTY);
        eventTypeNode = eventTypeNode == null ? payload.get(EVENT_TYPE_ALIAS_PROPERTY) : eventTypeNode;
        return eventTypeNode == null || eventTypeNode.isNull() ? null : eventTypeNode.asText();
    }
}
