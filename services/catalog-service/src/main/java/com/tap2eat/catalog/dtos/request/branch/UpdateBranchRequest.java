package com.tap2eat.catalog.dtos.request.branch;

import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;

public record UpdateBranchRequest(
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
        AvailabilityConfigRequest availability,
        Boolean isMainBranch
) {
}