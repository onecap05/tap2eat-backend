package com.tap2eat.notification.messaging.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentApprovedEvent(
        @JsonProperty("EventId") String eventId,
        @JsonProperty("EventType") String eventType,
        @JsonProperty("PaymentId") String paymentId,
        @JsonProperty("OrderId") String orderId,
        @JsonProperty("CustomerAccountId") String customerAccountId,
        @JsonProperty("RestaurantId") String restaurantId,
        @JsonProperty("BranchId") String branchId,
        @JsonProperty("Amount") BigDecimal amount,
        @JsonProperty("Currency") String currency,
        @JsonProperty("Status") String status,
        @JsonProperty("Provider") String provider,
        @JsonProperty("ProviderReference") String providerReference,
        @JsonProperty("OccurredAt") String occurredAt
) {
}
