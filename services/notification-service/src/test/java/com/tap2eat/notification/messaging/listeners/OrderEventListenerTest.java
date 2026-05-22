package com.tap2eat.notification.messaging.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.services.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private INotificationService notificationService;

    private OrderEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEventListener(new ObjectMapper(), notificationService);
    }

    @Test
    void whenOrderCreatedEventReceived_shouldCallNotificationService() {
        String rawMessage = """
                {
                  "EventId": "event-1",
                  "EventType": "order.created",
                  "OrderId": "order-1",
                  "CustomerAccountId": "customer-1",
                  "RestaurantId": "restaurant-1",
                  "BranchId": "branch-1",
                  "Subtotal": 120.50,
                  "Total": 140.75,
                  "Status": "CREATED",
                  "CreatedAt": "2026-05-22T10:15:30Z",
                  "OccurredAt": "2026-05-22T10:15:31Z"
                }
                """;

        listener.handleOrderEvent(rawMessage);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(notificationService).handleOrderCreated(eventCaptor.capture());

        OrderCreatedEvent event = eventCaptor.getValue();
        assertEquals("event-1", event.eventId());
        assertEquals("order.created", event.eventType());
        assertEquals("order-1", event.orderId());
        assertEquals("customer-1", event.customerAccountId());
        assertEquals("restaurant-1", event.restaurantId());
        assertEquals("branch-1", event.branchId());
        assertTrue(new BigDecimal("120.50").compareTo(event.subtotal()) == 0);
        assertTrue(new BigDecimal("140.75").compareTo(event.total()) == 0);
        assertEquals("CREATED", event.status());
        assertEquals("2026-05-22T10:15:30Z", event.createdAt());
        assertEquals("2026-05-22T10:15:31Z", event.occurredAt());
    }

    @Test
    void whenOrderStatusChangedEventReceived_shouldCallNotificationService() {
        String rawMessage = """
                {
                  "EventId": "event-2",
                  "EventType": "order.status.changed",
                  "OrderId": "order-2",
                  "CustomerAccountId": "customer-2",
                  "RestaurantId": "restaurant-2",
                  "BranchId": "branch-2",
                  "PreviousStatus": "CREATED",
                  "NewStatus": "CONFIRMED",
                  "OccurredAt": "2026-05-22T10:20:31Z"
                }
                """;

        listener.handleOrderEvent(rawMessage);

        ArgumentCaptor<OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(notificationService).handleOrderStatusChanged(eventCaptor.capture());

        OrderStatusChangedEvent event = eventCaptor.getValue();
        assertEquals("event-2", event.eventId());
        assertEquals("order.status.changed", event.eventType());
        assertEquals("order-2", event.orderId());
        assertEquals("customer-2", event.customerAccountId());
        assertEquals("restaurant-2", event.restaurantId());
        assertEquals("branch-2", event.branchId());
        assertEquals("CREATED", event.previousStatus());
        assertEquals("CONFIRMED", event.newStatus());
        assertEquals("2026-05-22T10:20:31Z", event.occurredAt());
    }

    @Test
    void whenUnknownEventTypeReceived_shouldNotThrow() {
        String rawMessage = """
                {
                  "EventType": "order.cancelled",
                  "OrderId": "order-3"
                }
                """;

        assertDoesNotThrow(() -> listener.handleOrderEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }

    @Test
    void whenInvalidJsonReceived_shouldHandleGracefully() {
        String rawMessage = "{ invalid-json";

        assertDoesNotThrow(() -> listener.handleOrderEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }
}
