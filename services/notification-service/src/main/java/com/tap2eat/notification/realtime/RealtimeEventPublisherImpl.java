package com.tap2eat.notification.realtime;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
import com.tap2eat.notification.realtime.messages.RealtimeOrderEventMessage;
import com.tap2eat.notification.realtime.messages.RealtimePaymentEventMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeEventPublisherImpl implements IRealtimeEventPublisher {

    private static final String CUSTOMER_ORDERS_TOPIC = "/topic/customers/%s/orders";
    private static final String RESTAURANT_ORDERS_TOPIC = "/topic/restaurants/%s/orders";
    private static final String CUSTOMER_PAYMENTS_TOPIC = "/topic/customers/%s/payments";
    private static final String RESTAURANT_PAYMENTS_TOPIC = "/topic/restaurants/%s/payments";

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisherImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        RealtimeOrderEventMessage message = new RealtimeOrderEventMessage(
                event.eventType(),
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.status(),
                event.status(),
                null,
                null,
                null,
                event.total(),
                event.occurredAt()
        );

        publishOrderMessage(event.customerAccountId(), event.restaurantId(), message);
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        RealtimeOrderEventMessage message = new RealtimeOrderEventMessage(
                event.eventType(),
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.newStatus(),
                event.newStatus(),
                event.previousStatus(),
                event.estimatedPreparationMinutes(),
                event.estimatedReadyAt(),
                null,
                event.occurredAt()
        );

        publishOrderMessage(event.customerAccountId(), event.restaurantId(), message);
    }

    @Override
    public void publishPaymentApproved(PaymentApprovedEvent event) {
        RealtimePaymentEventMessage message = new RealtimePaymentEventMessage(
                event.eventType(),
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.amount(),
                event.currency(),
                event.status(),
                null,
                event.occurredAt()
        );

        publishPaymentMessage(event.customerAccountId(), event.restaurantId(), message);
    }

    @Override
    public void publishPaymentRejected(PaymentRejectedEvent event) {
        RealtimePaymentEventMessage message = new RealtimePaymentEventMessage(
                event.eventType(),
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.amount(),
                event.currency(),
                event.status(),
                event.rejectionReason(),
                event.occurredAt()
        );

        publishPaymentMessage(event.customerAccountId(), event.restaurantId(), message);
    }

    @Override
    public void publishPaymentCancelled(PaymentCancelledEvent event) {
        RealtimePaymentEventMessage message = new RealtimePaymentEventMessage(
                event.eventType(),
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.amount(),
                event.currency(),
                event.status(),
                event.reason(),
                event.occurredAt()
        );

        publishPaymentMessage(event.customerAccountId(), event.restaurantId(), message);
    }

    private void publishOrderMessage(
            String customerAccountId,
            String restaurantId,
            RealtimeOrderEventMessage message
    ) {
        publishIfPresent(customerAccountId, CUSTOMER_ORDERS_TOPIC, message);
        publishIfPresent(restaurantId, RESTAURANT_ORDERS_TOPIC, message);
    }

    private void publishPaymentMessage(
            String customerAccountId,
            String restaurantId,
            RealtimePaymentEventMessage message
    ) {
        publishIfPresent(customerAccountId, CUSTOMER_PAYMENTS_TOPIC, message);
        publishIfPresent(restaurantId, RESTAURANT_PAYMENTS_TOPIC, message);
    }

    private void publishIfPresent(String destinationId, String topicTemplate, Object message) {
        if (destinationId == null || destinationId.isBlank()) {
            return;
        }

        messagingTemplate.convertAndSend(topicTemplate.formatted(destinationId), message);
    }
}
