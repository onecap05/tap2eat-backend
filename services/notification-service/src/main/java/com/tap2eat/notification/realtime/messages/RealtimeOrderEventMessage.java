package com.tap2eat.notification.realtime.messages;

import java.math.BigDecimal;

public record RealtimeOrderEventMessage(
        String eventType,
        String orderId,
        String customerAccountId,
        String restaurantId,
        String branchId,
        String status,
        String newStatus,
        String previousStatus,
        Integer estimatedPreparationMinutes,
        String estimatedReadyAt,
        BigDecimal total,
        String occurredAt
) {
}
