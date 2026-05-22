package com.tap2eat.notification.services;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;

public interface INotificationService {

    void handleOrderCreated(OrderCreatedEvent event);

    void handleOrderStatusChanged(OrderStatusChangedEvent event);
}
