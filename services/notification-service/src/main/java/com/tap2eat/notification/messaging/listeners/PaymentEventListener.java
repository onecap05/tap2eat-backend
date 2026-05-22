package com.tap2eat.notification.messaging.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
import com.tap2eat.notification.services.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
    private static final String PAYMENT_APPROVED_EVENT_TYPE = "payment.approved";
    private static final String PAYMENT_REJECTED_EVENT_TYPE = "payment.rejected";
    private static final String PAYMENT_CANCELLED_EVENT_TYPE = "payment.cancelled";
    private static final String EVENT_TYPE_PROPERTY = "EventType";

    private final ObjectMapper objectMapper;
    private final INotificationService notificationService;

    public PaymentEventListener(ObjectMapper objectMapper, INotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${tap2eat.rabbitmq.payments.queue-name:tap2eat.notifications.payments}")
    public void handlePaymentEvent(String rawMessage) {
        try {
            JsonNode payload = objectMapper.readTree(rawMessage);
            String eventType = getEventType(payload);

            if (PAYMENT_APPROVED_EVENT_TYPE.equals(eventType)) {
                PaymentApprovedEvent event = objectMapper.treeToValue(payload, PaymentApprovedEvent.class);
                notificationService.handlePaymentApproved(event);
                return;
            }

            if (PAYMENT_REJECTED_EVENT_TYPE.equals(eventType)) {
                PaymentRejectedEvent event = objectMapper.treeToValue(payload, PaymentRejectedEvent.class);
                notificationService.handlePaymentRejected(event);
                return;
            }

            if (PAYMENT_CANCELLED_EVENT_TYPE.equals(eventType)) {
                PaymentCancelledEvent event = objectMapper.treeToValue(payload, PaymentCancelledEvent.class);
                notificationService.handlePaymentCancelled(event);
                return;
            }

            log.warn("Ignoring unknown payment event type: {}", eventType);
        } catch (JsonProcessingException ex) {
            log.warn("Ignoring invalid payment event JSON payload: {}", ex.getOriginalMessage());
        }
    }

    private String getEventType(JsonNode payload) {
        JsonNode eventTypeNode = payload.get(EVENT_TYPE_PROPERTY);
        return eventTypeNode == null || eventTypeNode.isNull() ? null : eventTypeNode.asText();
    }
}
