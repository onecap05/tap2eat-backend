package com.tap2eat.notification.messaging.events;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusChangedEvent(
        @JsonProperty("EventId") @JsonAlias("eventId") String eventId,
        @JsonProperty("EventType") @JsonAlias("eventType") String eventType,
        @JsonProperty("OrderId") @JsonAlias("orderId") String orderId,
        @JsonProperty("CustomerAccountId") @JsonAlias("customerAccountId") String customerAccountId,
        @JsonProperty("RestaurantId") @JsonAlias("restaurantId") String restaurantId,
        @JsonProperty("BranchId") @JsonAlias("branchId") String branchId,
        @JsonProperty("PreviousStatus") @JsonAlias("previousStatus") String previousStatus,
        @JsonProperty("NewStatus") @JsonAlias({"newStatus", "status"}) String newStatus,
        @JsonProperty("EstimatedPreparationMinutes") @JsonAlias("estimatedPreparationMinutes") Integer estimatedPreparationMinutes,
        @JsonProperty("EstimatedReadyAt") @JsonAlias("estimatedReadyAt") String estimatedReadyAt,
        @JsonProperty("OccurredAt") @JsonAlias("occurredAt") String occurredAt
) {
}
