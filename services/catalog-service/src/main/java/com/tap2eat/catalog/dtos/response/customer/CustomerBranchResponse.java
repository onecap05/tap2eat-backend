package com.tap2eat.catalog.dtos.response.customer;

import com.tap2eat.catalog.dtos.response.common.AvailabilityConfigResponse;

public record CustomerBranchResponse(
        String id,
        String restaurantId,
        String name,
        String phoneNumber,
        String formattedAddress,
        String street,
        String exteriorNumber,
        String interiorNumber,
        String neighborhood,
        String city,
        String state,
        String postalCode,
        String country,
        String addressReference,
        Double latitude,
        Double longitude,
        String googlePlaceId,
        AvailabilityConfigResponse availability,
        Boolean isMainBranch,
        Boolean active,
        Boolean open
) {
}
