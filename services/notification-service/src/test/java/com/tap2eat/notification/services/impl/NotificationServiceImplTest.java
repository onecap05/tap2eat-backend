package com.tap2eat.notification.services.impl;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceImplTest {

    private final NotificationServiceImpl notificationService = new NotificationServiceImpl();

    @Test
    void handleOrderCreated_shouldNotThrow() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "event-1",
                "order.created",
                "order-1",
                "customer-1",
                "restaurant-1",
                "branch-1",
                new BigDecimal("120.50"),
                new BigDecimal("140.75"),
                "CREATED",
                "2026-05-22T10:15:30Z",
                "2026-05-22T10:15:31Z"
        );

        assertDoesNotThrow(() -> notificationService.handleOrderCreated(event));
    }

    @Test
    void handleOrderStatusChanged_shouldNotThrow() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                "event-2",
                "order.status.changed",
                "order-2",
                "customer-2",
                "restaurant-2",
                "branch-2",
                "CREATED",
                "CONFIRMED",
                "2026-05-22T10:20:31Z"
        );

        assertDoesNotThrow(() -> notificationService.handleOrderStatusChanged(event));
    }

    @Test
    void handlePaymentApproved_shouldNotThrow() {
        PaymentApprovedEvent event = new PaymentApprovedEvent(
                "event-3",
                "payment.approved",
                "payment-1",
                "order-1",
                "customer-1",
                "restaurant-1",
                "branch-1",
                new BigDecimal("140.75"),
                "MXN",
                "Approved",
                "SIMULATED",
                "provider-ref-1",
                "2026-05-22T10:25:31Z"
        );

        assertDoesNotThrow(() -> notificationService.handlePaymentApproved(event));
    }

    @Test
    void handlePaymentRejected_shouldNotThrow() {
        PaymentRejectedEvent event = new PaymentRejectedEvent(
                "event-4",
                "payment.rejected",
                "payment-2",
                "order-2",
                "customer-2",
                "restaurant-2",
                "branch-2",
                new BigDecimal("99.90"),
                "MXN",
                "Rejected",
                "Declined",
                "2026-05-22T10:30:31Z"
        );

        assertDoesNotThrow(() -> notificationService.handlePaymentRejected(event));
    }

    @Test
    void handlePaymentCancelled_shouldNotThrow() {
        PaymentCancelledEvent event = new PaymentCancelledEvent(
                "event-5",
                "payment.cancelled",
                "payment-3",
                "order-3",
                "customer-3",
                "restaurant-3",
                "branch-3",
                new BigDecimal("75.00"),
                "MXN",
                "Cancelled",
                "ORDER_CANCELLED",
                "2026-05-22T10:35:31Z"
        );

        assertDoesNotThrow(() -> notificationService.handlePaymentCancelled(event));
    }
}
