package com.tap2eat.notification.messaging.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
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
class PaymentEventListenerTest {

    @Mock
    private INotificationService notificationService;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(new ObjectMapper(), notificationService);
    }

    @Test
    void whenPaymentApprovedEventReceived_shouldCallNotificationService() {
        String rawMessage = """
                {
                  "EventId": "event-1",
                  "EventType": "payment.approved",
                  "PaymentId": "payment-1",
                  "OrderId": "order-1",
                  "CustomerAccountId": "customer-1",
                  "RestaurantId": "restaurant-1",
                  "BranchId": "branch-1",
                  "Amount": 140.75,
                  "Currency": "MXN",
                  "Status": "Approved",
                  "Provider": "SIMULATED",
                  "ProviderReference": "provider-ref-1",
                  "OccurredAt": "2026-05-22T10:25:31Z"
                }
                """;

        listener.handlePaymentEvent(rawMessage);

        ArgumentCaptor<PaymentApprovedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(notificationService).handlePaymentApproved(eventCaptor.capture());

        PaymentApprovedEvent event = eventCaptor.getValue();
        assertEquals("event-1", event.eventId());
        assertEquals("payment.approved", event.eventType());
        assertEquals("payment-1", event.paymentId());
        assertEquals("order-1", event.orderId());
        assertEquals("customer-1", event.customerAccountId());
        assertTrue(new BigDecimal("140.75").compareTo(event.amount()) == 0);
        assertEquals("provider-ref-1", event.providerReference());
    }

    @Test
    void whenPaymentRejectedEventReceived_shouldCallNotificationService() {
        String rawMessage = """
                {
                  "EventId": "event-2",
                  "EventType": "payment.rejected",
                  "PaymentId": "payment-2",
                  "OrderId": "order-2",
                  "CustomerAccountId": "customer-2",
                  "RestaurantId": "restaurant-2",
                  "BranchId": "branch-2",
                  "Amount": 99.90,
                  "Currency": "MXN",
                  "Status": "Rejected",
                  "RejectionReason": "Declined",
                  "OccurredAt": "2026-05-22T10:30:31Z"
                }
                """;

        listener.handlePaymentEvent(rawMessage);

        ArgumentCaptor<PaymentRejectedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRejectedEvent.class);
        verify(notificationService).handlePaymentRejected(eventCaptor.capture());

        PaymentRejectedEvent event = eventCaptor.getValue();
        assertEquals("event-2", event.eventId());
        assertEquals("payment.rejected", event.eventType());
        assertEquals("payment-2", event.paymentId());
        assertEquals("order-2", event.orderId());
        assertTrue(new BigDecimal("99.90").compareTo(event.amount()) == 0);
        assertEquals("Declined", event.rejectionReason());
    }

    @Test
    void whenPaymentCancelledEventReceived_shouldCallNotificationService() {
        String rawMessage = """
                {
                  "EventId": "event-3",
                  "EventType": "payment.cancelled",
                  "PaymentId": "payment-3",
                  "OrderId": "order-3",
                  "CustomerAccountId": "customer-3",
                  "RestaurantId": "restaurant-3",
                  "BranchId": "branch-3",
                  "Amount": 75.00,
                  "Currency": "MXN",
                  "Status": "Cancelled",
                  "Reason": "ORDER_CANCELLED",
                  "OccurredAt": "2026-05-22T10:35:31Z"
                }
                """;

        listener.handlePaymentEvent(rawMessage);

        ArgumentCaptor<PaymentCancelledEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCancelledEvent.class);
        verify(notificationService).handlePaymentCancelled(eventCaptor.capture());

        PaymentCancelledEvent event = eventCaptor.getValue();
        assertEquals("event-3", event.eventId());
        assertEquals("payment.cancelled", event.eventType());
        assertEquals("payment-3", event.paymentId());
        assertEquals("order-3", event.orderId());
        assertTrue(new BigDecimal("75.00").compareTo(event.amount()) == 0);
        assertEquals("ORDER_CANCELLED", event.reason());
    }

    @Test
    void whenUnknownPaymentEventReceived_shouldNotThrow() {
        String rawMessage = """
                {
                  "EventType": "payment.refunded",
                  "PaymentId": "payment-4"
                }
                """;

        assertDoesNotThrow(() -> listener.handlePaymentEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }

    @Test
    void whenInvalidJsonReceived_shouldNotThrow() {
        String rawMessage = "{ invalid-json";

        assertDoesNotThrow(() -> listener.handlePaymentEvent(rawMessage));

        verifyNoInteractions(notificationService);
    }
}
