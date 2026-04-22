package com.tap2eat.catalog.models.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierOption {

    private String id;
    private String name;
    private BigDecimal priceDelta = BigDecimal.ZERO;
    private Boolean isActive = true;
    private Integer displayOrder;
}