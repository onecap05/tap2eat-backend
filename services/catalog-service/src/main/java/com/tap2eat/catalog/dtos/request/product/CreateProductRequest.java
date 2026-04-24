package com.tap2eat.catalog.dtos.request.product;

import com.tap2eat.catalog.models.enums.ProductType;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        String restaurantId,
        String categoryId,
        String name,
        String description,
        ProductType productType,
        BigDecimal price,
        ImageMetadataRequest image,
        List<ModifierGroupRequest> modifierGroups,
        AvailabilityConfigRequest availability,
        Boolean active,
        Integer displayOrder,
        Boolean featured,
        List<String> tags,
        List<String> dietaryFlags,
        List<String> allergens
) {
}