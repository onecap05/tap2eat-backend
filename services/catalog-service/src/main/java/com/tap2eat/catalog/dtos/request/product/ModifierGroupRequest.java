package com.tap2eat.catalog.dtos.request.product;

import com.tap2eat.catalog.models.enums.SelectionType;

import java.util.List;

public record ModifierGroupRequest(
        String name,
        SelectionType selectionType,
        Integer minSelections,
        Integer maxSelections,
        Boolean required,
        Boolean active,
        Integer displayOrder,
        List<ModifierOptionRequest> options
) {
}