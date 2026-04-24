package com.tap2eat.catalog.dtos.request.category;

import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;

public record UpdateCategoryRequest(
        String name,
        String description,
        Integer displayOrder,
        ImageMetadataRequest image,
        AvailabilityConfigRequest availability
) {
}