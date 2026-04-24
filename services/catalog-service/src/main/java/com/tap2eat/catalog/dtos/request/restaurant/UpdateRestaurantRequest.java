package com.tap2eat.catalog.dtos.request.restaurant;

import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;

public record UpdateRestaurantRequest(
        String name,
        String description,
        ImageMetadataRequest logo
) {
}