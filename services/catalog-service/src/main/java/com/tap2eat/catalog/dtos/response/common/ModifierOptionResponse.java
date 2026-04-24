package com.tap2eat.catalog.dtos.response.common;

import java.math.BigDecimal;

public record ModifierOptionResponse(
        String id,
        String name,
        BigDecimal additionalPrice,
        Boolean active,
        Integer displayOrder
) {
}