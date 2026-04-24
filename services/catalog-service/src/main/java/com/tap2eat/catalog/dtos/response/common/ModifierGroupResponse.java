package com.tap2eat.catalog.dtos.response.common;

import com.tap2eat.catalog.models.enums.SelectionType;

import java.util.List;

public record ModifierGroupResponse(
        String id,
        String name,
        SelectionType selectionType,
        Boolean required,
        Integer minSelections,
        Integer maxSelections,
        Integer displayOrder,
        Boolean active,
        List<ModifierOptionResponse> options
) {
}