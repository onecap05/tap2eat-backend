package com.tap2eat.notification.services.impl;

import com.tap2eat.notification.messaging.events.OrderCreatedEvent;
import com.tap2eat.notification.messaging.events.OrderStatusChangedEvent;
import com.tap2eat.notification.messaging.events.PaymentApprovedEvent;
import com.tap2eat.notification.messaging.events.PaymentCancelledEvent;
import com.tap2eat.notification.messaging.events.PaymentRejectedEvent;
import com.tap2eat.notification.realtime.IRealtimeEventPublisher;
import com.tap2eat.notification.services.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements INotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final IRealtimeEventPublisher realtimeEventPublisher;

    public NotificationServiceImpl(IRealtimeEventPublisher realtimeEventPublisher) {
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Override
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info(
                "Publishing realtime notification for order.created: orderId={}, customerAccountId={}, restaurantId={}, branchId={}, total={}, status={}",
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.branchId(),
                event.total(),
                event.status()
        );
        realtimeEventPublisher.publishOrderCreated(event);
    }

    @Override
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info(
                "Publishing realtime notification for order.status.changed: orderId={}, customerAccountId={}, restaurantId={}, previousStatus={}, newStatus={}",
                event.orderId(),
                event.customerAccountId(),
                event.restaurantId(),
                event.previousStatus(),
                event.newStatus()
        );
        realtimeEventPublisher.publishOrderStatusChanged(event);
    }

    @Override
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        log.info(
                "Publishing realtime notification for payment.approved: paymentId={}, orderId={}, customerAccountId={}, amount={}, providerReference={}",
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.amount(),
                event.providerReference()
        );
        realtimeEventPublisher.publishPaymentApproved(event);
    }

    @Override
    public void handlePaymentRejected(PaymentRejectedEvent event) {
        log.info(
                "Publishing realtime notification for payment.rejected: paymentId={}, orderId={}, customerAccountId={}, amount={}, rejectionReason={}",
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.amount(),
                event.rejectionReason()
        );
        realtimeEventPublisher.publishPaymentRejected(event);
    }

    @Override
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        log.info(
                "Publishing realtime notification for payment.cancelled: paymentId={}, orderId={}, customerAccountId={}, amount={}, reason={}",
                event.paymentId(),
                event.orderId(),
                event.customerAccountId(),
                event.amount(),
                event.reason()
        );
        realtimeEventPublisher.publishPaymentCancelled(event);
    }
}
