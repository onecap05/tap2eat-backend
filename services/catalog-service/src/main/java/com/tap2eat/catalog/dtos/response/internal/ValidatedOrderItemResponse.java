package com.tap2eat.catalog.dtos.response.internal;

import java.math.BigDecimal;
import java.util.List;

public record ValidatedOrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        List<ValidatedModifierResponse> selectedModifiers,
        BigDecimal subtotal
) {
}
