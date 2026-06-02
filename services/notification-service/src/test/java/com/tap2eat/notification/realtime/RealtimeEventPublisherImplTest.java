package com.tap2eat.notification.realtime;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
import com.tap2eat.notification.realtime.messages.RealtimeOrderEventMessage;
import com.tap2eat.notification.realtime.messages.RealtimePaymentEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RealtimeEventPublisherImplTest {

    private SimpMessagingTemplate messagingTemplate;
    private RealtimeEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        publisher = new RealtimeEventPublisherImpl(messagingTemplate);
    }

    @Test
    void publishOrderCreated_shouldPublishToCustomerAndRestaurantTopics() {
        OrderCreatedEvent event = orderCreatedEvent("customer-1", "restaurant-1");

        publisher.publishOrderCreated(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-1/orders"),
                any(RealtimeOrderEventMessage.class)
        );
        ArgumentCaptor<RealtimeOrderEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimeOrderEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-1/orders"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().eventType()).isEqualTo("order.created");
        assertThat(messageCaptor.getValue().status()).isEqualTo("Created");
        assertThat(messageCaptor.getValue().newStatus()).isEqualTo("Created");
        assertThat(messageCaptor.getValue().previousStatus()).isNull();
        assertThat(messageCaptor.getValue().estimatedPreparationMinutes()).isNull();
        assertThat(messageCaptor.getValue().estimatedReadyAt()).isNull();
        assertThat(messageCaptor.getValue().total()).isEqualByComparingTo("140.75");
    }

    @Test
    void publishOrderStatusChanged_shouldPublishToCustomerAndRestaurantTopics() {
        OrderStatusChangedEvent event = orderStatusChangedEvent("customer-2", "restaurant-2");

        publisher.publishOrderStatusChanged(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-2/orders"),
                any(RealtimeOrderEventMessage.class)
        );
        ArgumentCaptor<RealtimeOrderEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimeOrderEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-2/orders"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().eventType()).isEqualTo("order.status.changed");
        assertThat(messageCaptor.getValue().status()).isEqualTo("Preparing");
        assertThat(messageCaptor.getValue().newStatus()).isEqualTo("Preparing");
        assertThat(messageCaptor.getValue().previousStatus()).isEqualTo("Accepted");
        assertThat(messageCaptor.getValue().estimatedPreparationMinutes()).isEqualTo(20);
        assertThat(messageCaptor.getValue().estimatedReadyAt()).isEqualTo("2026-05-22T10:40:31Z");
        assertThat(messageCaptor.getValue().total()).isNull();
    }

    @Test
    void publishPaymentApproved_shouldPublishToCustomerAndRestaurantTopics() {
        PaymentApprovedEvent event = paymentApprovedEvent("customer-3", "restaurant-3");

        publisher.publishPaymentApproved(event);

        verifyPaymentTopics("customer-3", "restaurant-3");
    }

    @Test
    void publishPaymentRejected_shouldPublishToCustomerAndRestaurantTopics() {
        PaymentRejectedEvent event = paymentRejectedEvent("customer-4", "restaurant-4");

        publisher.publishPaymentRejected(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-4/payments"),
                any(RealtimePaymentEventMessage.class)
        );
        ArgumentCaptor<RealtimePaymentEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimePaymentEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-4/payments"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().eventType()).isEqualTo("payment.rejected");
        assertThat(messageCaptor.getValue().reason()).isEqualTo("Declined");
    }

    @Test
    void publishPaymentCancelled_shouldPublishToCustomerAndRestaurantTopics() {
        PaymentCancelledEvent event = paymentCancelledEvent("customer-5", "restaurant-5");

        publisher.publishPaymentCancelled(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-5/payments"),
                any(RealtimePaymentEventMessage.class)
        );
        ArgumentCaptor<RealtimePaymentEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimePaymentEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-5/payments"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().eventType()).isEqualTo("payment.cancelled");
        assertThat(messageCaptor.getValue().reason()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void publishOrderCreated_shouldSkipCustomerTopicWhenCustomerAccountIdIsBlank() {
        OrderCreatedEvent event = orderCreatedEvent(" ", "restaurant-1");

        publisher.publishOrderCreated(event);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/customers/ /orders"),
                any(Object.class)
        );
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/restaurant-1/orders"),
                any(RealtimeOrderEventMessage.class)
        );
    }

    @Test
    void publishPaymentApproved_shouldSkipRestaurantTopicWhenRestaurantIdIsBlank() {
        PaymentApprovedEvent event = paymentApprovedEvent("customer-1", "");

        publisher.publishPaymentApproved(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/customer-1/payments"),
                any(RealtimePaymentEventMessage.class)
        );
        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/restaurants//payments"),
                any(Object.class)
        );
    }

    private void verifyPaymentTopics(String customerAccountId, String restaurantId) {
        verify(messagingTemplate).convertAndSend(
                eq("/topic/customers/%s/payments".formatted(customerAccountId)),
                any(RealtimePaymentEventMessage.class)
        );
        ArgumentCaptor<RealtimePaymentEventMessage> messageCaptor = ArgumentCaptor.forClass(RealtimePaymentEventMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/%s/payments".formatted(restaurantId)),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().eventType()).isEqualTo("payment.approved");
        assertThat(messageCaptor.getValue().reason()).isNull();
    }

    private static OrderCreatedEvent orderCreatedEvent(String customerAccountId, String restaurantId) {
        return new OrderCreatedEvent(
                "event-1",
                "order.created",
                "order-1",
                customerAccountId,
                restaurantId,
                "branch-1",
                new BigDecimal("120.50"),
                new BigDecimal("140.75"),
                "Created",
                "2026-05-22T10:15:30Z",
                "2026-05-22T10:15:31Z"
        );
    }

    private static OrderStatusChangedEvent orderStatusChangedEvent(String customerAccountId, String restaurantId) {
        return new OrderStatusChangedEvent(
                "event-2",
                "order.status.changed",
                "order-2",
                customerAccountId,
                restaurantId,
                "branch-2",
                "Accepted",
                "Preparing",
                20,
                "2026-05-22T10:40:31Z",
                "2026-05-22T10:20:31Z"
        );
    }

    private static PaymentApprovedEvent paymentApprovedEvent(String customerAccountId, String restaurantId) {
        return new PaymentApprovedEvent(
                "event-3",
                "payment.approved",
                "payment-1",
                "order-1",
                customerAccountId,
                restaurantId,
                "branch-1",
                new BigDecimal("140.75"),
                "MXN",
                "Approved",
                "SIMULATED",
                "provider-ref-1",
                "2026-05-22T10:25:31Z"
        );
    }

    private static PaymentRejectedEvent paymentRejectedEvent(String customerAccountId, String restaurantId) {
        return new PaymentRejectedEvent(
                "event-4",
                "payment.rejected",
                "payment-2",
                "order-2",
                customerAccountId,
                restaurantId,
                "branch-2",
                new BigDecimal("99.90"),
                "MXN",
                "Rejected",
                "Declined",
                "2026-05-22T10:30:31Z"
        );
    }

    private static PaymentCancelledEvent paymentCancelledEvent(String customerAccountId, String restaurantId) {
        return new PaymentCancelledEvent(
                "event-5",
                "payment.cancelled",
                "payment-3",
                "order-3",
                customerAccountId,
                restaurantId,
                "branch-3",
                new BigDecimal("75.00"),
                "MXN",
                "Cancelled",
                "ORDER_CANCELLED",
                "2026-05-22T10:35:31Z"
        );
    }
}
