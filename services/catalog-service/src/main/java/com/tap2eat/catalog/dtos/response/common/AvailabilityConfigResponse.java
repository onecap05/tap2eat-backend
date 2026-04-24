package com.tap2eat.catalog.dtos.response.common;

import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;

import java.util.List;

public record AvailabilityConfigResponse(
        AvailabilityStatus status,
        TemporaryUnavailabilityReason temporaryReason,
        String temporaryReasonDetail,
        List<DailyAvailabilityResponse> weeklySchedule
) {
}