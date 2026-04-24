package com.tap2eat.catalog.dtos.response.category;

import com.tap2eat.catalog.dtos.response.common.AvailabilityConfigResponse;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;

import java.time.LocalDateTime;

public record CategoryResponse(
        String id,
        String restaurantId,
        String name,
        String description,
        Integer displayOrder,
        ImageMetadataResponse image,
        AvailabilityConfigResponse availability,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}