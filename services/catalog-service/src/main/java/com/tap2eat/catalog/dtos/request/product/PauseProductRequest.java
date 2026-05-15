package com.tap2eat.catalog.dtos.request.product;

import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;

public record PauseProductRequest(
        TemporaryUnavailabilityReason temporaryReason,
        String temporaryReasonDetail
) {
}