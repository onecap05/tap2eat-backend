package com.tap2eat.notification.messaging.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderStatusChangedEvent(
        @JsonProperty("EventId") String eventId,
        @JsonProperty("EventType") String eventType,
        @JsonProperty("OrderId") String orderId,
        @JsonProperty("CustomerAccountId") String customerAccountId,
        @JsonProperty("RestaurantId") String restaurantId,
        @JsonProperty("BranchId") String branchId,
        @JsonProperty("PreviousStatus") String previousStatus,
        @JsonProperty("NewStatus") String newStatus,
        @JsonProperty("OccurredAt") String occurredAt
) {
}
