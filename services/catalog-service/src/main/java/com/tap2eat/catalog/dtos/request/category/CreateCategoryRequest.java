package com.tap2eat.catalog.dtos.request.category;

import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;

public record CreateCategoryRequest(
        String restaurantId,
        String name,
        String description,
        Integer displayOrder,
        ImageMetadataRequest image,
        AvailabilityConfigRequest availability
) {
}