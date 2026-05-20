package com.tap2eat.catalog.dtos.response.customer;

import com.tap2eat.catalog.dtos.response.common.AvailabilityConfigResponse;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import com.tap2eat.catalog.dtos.response.common.ModifierGroupResponse;
import com.tap2eat.catalog.models.enums.ProductType;

import java.math.BigDecimal;
import java.util.List;

public record CustomerProductResponse(
        String id,
        String restaurantId,
        String categoryId,
        String name,
        String description,
        ProductType productType,
        BigDecimal price,
        ImageMetadataResponse image,
        Integer displayOrder,
        Boolean featured,
        AvailabilityConfigResponse availability,
        Boolean active,
        Boolean available,
        List<String> tags,
        List<String> dietaryFlags,
        List<String> allergens,
        List<ModifierGroupResponse> modifierGroups
) {
}
