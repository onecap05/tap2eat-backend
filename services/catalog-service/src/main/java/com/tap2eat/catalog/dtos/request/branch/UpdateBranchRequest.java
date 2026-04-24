package com.tap2eat.catalog.dtos.request.branch;

import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;

public record UpdateBranchRequest(
        String name,
        String phoneNumber,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String googlePlaceId,
        AvailabilityConfigRequest availability,
        Boolean isMainBranch
) {
}