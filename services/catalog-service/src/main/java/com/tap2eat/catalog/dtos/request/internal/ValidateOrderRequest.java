package com.tap2eat.catalog.dtos.request.internal;

import java.util.List;

public record ValidateOrderRequest(
        String restaurantId,
        String branchId,
        List<ValidateOrderItemRequest> items
) {
}
