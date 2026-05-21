package com.tap2eat.catalog.dtos.response.internal;

import java.math.BigDecimal;
import java.util.List;

public record ValidateOrderResponse(
        String restaurantId,
        String branchId,
        boolean valid,
        List<ValidatedOrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal total
) {
}
