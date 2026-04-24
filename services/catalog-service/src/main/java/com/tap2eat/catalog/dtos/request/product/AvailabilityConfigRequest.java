package com.tap2eat.catalog.dtos.request.product;

import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;

import java.util.List;

public record AvailabilityConfigRequest(
        AvailabilityStatus status,
        TemporaryUnavailabilityReason temporaryReason,
        String temporaryReasonDetail,
        List<DailyAvailabilityRequest> weeklySchedule
) {
}