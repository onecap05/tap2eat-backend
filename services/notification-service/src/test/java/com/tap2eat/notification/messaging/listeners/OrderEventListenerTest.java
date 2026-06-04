package com.tap2eat.notification.messaging.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.realtime.RealtimeEventPublisherImpl;
import com.tap2eat.notification.realtime.messages.RealtimeOrderEventMessage;
import com.tap2eat.notification.services.INotificationService;
import com.tap2eat.notification.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

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
                  "EstimatedPreparationMinutes": 20,
                  "EstimatedReadyAt": "2026-05-22T10:40:31Z",
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
        assertEquals(20, event.estimatedPreparationMinutes());
        assertEquals("2026-05-22T10:40:31Z", event.estimatedReadyAt());
        assertEquals("2026-05-22T10:20:31Z", event.occurredAt());
    }

    @Test
    void whenOrderStatusChangedEventContainsItems_shouldPublishRealtimeOrderTopics() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        OrderEventListener realtimeListener = new OrderEventListener(
                new ObjectMapper(),
                new NotificationServiceImpl(new RealtimeEventPublisherImpl(messagingTemplate))
        );
        String rawMessage = """
                {
                  "EventId": "event-3",
                  "EventType": "order.status.changed",
                  "OrderId": "order-3",
                  "CustomerAccountId": "customer-3",
                  "RestaurantId": "restaurant-3",
                  "BranchId": "branch-3",
                  "PreviousStatus": "Created",
                  "NewStatus": "Accepted",
                  "EstimatedPreparationMinutes": 20,
                  "EstimatedReadyAt": "2026-05-22T10:40:31Z",
                  "Items": [
                    {
                      "ProductId": "product-1",
                      "Quantity": 2,
                      "ProductNameSnapshot": "Taco"
                    }
                  ],
                  "OccurredAt": "2026-05-22T10:20:31Z"
                }
                """;

        realtimeListener.handleOrderEvent(rawMessage);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-3/orders"),
                any(RealtimeOrderEventMessage.class)
        );
        ArgumentCaptor<RealtimeOrderEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimeOrderEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-3/orders"),
                messageCaptor.capture()
        );
        RealtimeOrderEventMessage message = messageCaptor.getValue();
        assertEquals("order.status.changed", message.eventType());
        assertEquals("order-3", message.orderId());
        assertEquals("customer-3", message.customerAccountId());
        assertEquals("restaurant-3", message.restaurantId());
        assertEquals("Accepted", message.status());
        assertEquals("Accepted", message.newStatus());
        assertEquals("Created", message.previousStatus());
        assertEquals(20, message.estimatedPreparationMinutes());
        assertEquals("2026-05-22T10:40:31Z", message.estimatedReadyAt());
        assertEquals("2026-05-22T10:20:31Z", message.occurredAt());
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
    void whenEventTypeAliasIsUnknown_shouldIgnoreMessage() {
        String rawMessage = """
                {
                  "EventId": "event-alias",
                  "eventType": "order.cancelled",
                  "OrderId": "order-alias"
                }
                """;

        assertDoesNotThrow(() -> listener.handleOrderEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }

    @Test
    void whenEventTypeIsMissing_shouldIgnoreMessage() {
        String rawMessage = """
                {
                  "OrderId": "order-4"
                }
                """;

        assertDoesNotThrow(() -> listener.handleOrderEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }

    @Test
    void whenNotificationServiceFails_shouldPropagateError() {
        String rawMessage = """
                {
                  "EventId": "event-error",
                  "EventType": "order.created",
                  "OrderId": "order-error",
                  "CustomerAccountId": "customer-error",
                  "RestaurantId": "restaurant-error",
                  "BranchId": "branch-error",
                  "Subtotal": 10.00,
                  "Total": 12.00,
                  "Status": "CREATED",
                  "CreatedAt": "2026-05-22T10:15:30Z",
                  "OccurredAt": "2026-05-22T10:15:31Z"
                }
                """;
        RuntimeException failure = new RuntimeException("realtime unavailable");
        doThrow(failure).when(notificationService).handleOrderCreated(any(OrderCreatedEvent.class));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> listener.handleOrderEvent(rawMessage));

        assertEquals(failure, thrown);
    }

    @Test
    void whenInvalidJsonReceived_shouldHandleGracefully() {
        String rawMessage = "{ invalid-json";

        assertDoesNotThrow(() -> listener.handleOrderEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }
}
