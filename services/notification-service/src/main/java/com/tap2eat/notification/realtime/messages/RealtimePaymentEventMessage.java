package com.tap2eat.notification.realtime.messages;

import java.math.BigDecimal;

public record RealtimePaymentEventMessage(
        String eventType,
        String paymentId,
        String orderId,
        String customerAccountId,
        String restaurantId,
        String branchId,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        String occurredAt
) {
}
