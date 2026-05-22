package com.tap2eat.notification.services.impl;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.services.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements INotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info(
                "Simulated notification for order.created: orderId={}, customerAccountId={}, restaurantId={}, branchId={}, total={}, status={}",
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.total(),
                event.status()
        );
    }

    @Override
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info(
                "Simulated notification for order.status.changed: orderId={}, customerAccountId={}, restaurantId={}, previousStatus={}, newStatus={}",
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.previousStatus(),
                event.newStatus()
        );
    }
}
