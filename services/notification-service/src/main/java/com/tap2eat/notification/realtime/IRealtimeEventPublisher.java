package com.tap2eat.notification.realtime;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;

public interface IRealtimeEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderStatusChanged(OrderStatusChangedEvent event);

    void publishPaymentApproved(PaymentApprovedEvent event);

    void publishPaymentRejected(PaymentRejectedEvent event);

    void publishPaymentCancelled(PaymentCancelledEvent event);
}
