package com.tap2eat.catalog.dtos.request.internal;

import java.util.List;

public record ValidateOrderItemRequest(
        String productId,
        Integer quantity,
        List<String> selectedModifierOptionIds
) {
}
