package com.tap2eat.catalog.dtos.response.branch;

import com.tap2eat.catalog.dtos.response.common.AvailabilityConfigResponse;

import java.time.LocalDateTime;

public record BranchResponse(
        String id,
        String restaurantId,
        String name,
        String phoneNumber,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String googlePlaceId,
        AvailabilityConfigResponse availability,
        Boolean isMainBranch,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}