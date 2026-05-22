package com.tap2eat.notification.messaging.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        @JsonProperty("EventId") String eventId,
        @JsonProperty("EventType") String eventType,
        @JsonProperty("OrderId") String orderId,
        @JsonProperty("CustomerAccountId") String customerAccountId,
        @JsonProperty("RestaurantId") String restaurantId,
        @JsonProperty("BranchId") String branchId,
        @JsonProperty("Subtotal") BigDecimal subtotal,
        @JsonProperty("Total") BigDecimal total,
        @JsonProperty("Status") String status,
        @JsonProperty("CreatedAt") String createdAt,
        @JsonProperty("OccurredAt") String occurredAt
) {
}
