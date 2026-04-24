package com.tap2eat.catalog.dtos.request.product;

import java.math.BigDecimal;

public record ModifierOptionRequest(
        String name,
        BigDecimal additionalPrice,
        Boolean active,
        Integer displayOrder
) {
}