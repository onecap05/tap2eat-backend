package com.tap2eat.catalog.dtos.response.restaurant;

import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;

import java.time.LocalDateTime;

public record RestaurantResponse(
        String id,
        String ownerAccountId,
        String name,
        String description,
        String rfc,
        ImageMetadataResponse logo,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
