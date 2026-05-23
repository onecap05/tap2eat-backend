package com.tap2eat.notification.realtime.messages;

import java.math.BigDecimal;

public record RealtimeOrderEventMessage(
        String eventType,
        String orderId,
        String customerAccountId,
        String restaurantId,
        String branchId,
        String status,
        String previousStatus,
        BigDecimal total,
        String occurredAt
) {
}
