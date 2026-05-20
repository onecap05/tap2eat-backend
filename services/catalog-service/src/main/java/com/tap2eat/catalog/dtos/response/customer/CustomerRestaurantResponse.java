package com.tap2eat.catalog.dtos.response.customer;

import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;

public record CustomerRestaurantResponse(
        String id,
        String name,
        String description,
        ImageMetadataResponse logo,
        Boolean active,
        Boolean open
) {
}
