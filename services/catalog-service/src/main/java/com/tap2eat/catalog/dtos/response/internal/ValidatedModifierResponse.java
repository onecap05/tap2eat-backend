package com.tap2eat.catalog.dtos.response.internal;

import java.math.BigDecimal;

public record ValidatedModifierResponse(
        String modifierGroupId,
        String modifierGroupName,
        String modifierOptionId,
        String modifierOptionName,
        BigDecimal priceAdjustment
) {
}
