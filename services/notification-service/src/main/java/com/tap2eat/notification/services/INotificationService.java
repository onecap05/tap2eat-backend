package com.tap2eat.notification.services;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;

public interface INotificationService {

    void handleOrderCreated(OrderCreatedEvent event);

    void handleOrderStatusChanged(OrderStatusChangedEvent event);

    void handlePaymentApproved(PaymentApprovedEvent event);

    void handlePaymentRejected(PaymentRejectedEvent event);

    void handlePaymentCancelled(PaymentCancelledEvent event);
}
