package com.tap2eat.catalog.dtos.response.customer;

import com.tap2eat.catalog.dtos.response.common.AvailabilityConfigResponse;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;

public record CustomerCategoryResponse(
        String id,
        String restaurantId,
        String name,
        String description,
        Integer displayOrder,
        ImageMetadataResponse image,
        AvailabilityConfigResponse availability,
        Boolean active,
        Boolean available
) {
}
