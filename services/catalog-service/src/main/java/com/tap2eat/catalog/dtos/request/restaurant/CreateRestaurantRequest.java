package com.tap2eat.catalog.dtos.request.restaurant;

import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;

public record CreateRestaurantRequest(
        String ownerAccountId,
        String name,
        String description,
        String rfc,
        ImageMetadataRequest logo
) {
}
