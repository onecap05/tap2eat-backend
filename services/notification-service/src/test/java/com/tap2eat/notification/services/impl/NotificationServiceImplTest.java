package com.tap2eat.notification.services.impl;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
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
}
