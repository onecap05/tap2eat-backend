package com.tap2eat.catalog.dtos.response.common;

import java.time.DayOfWeek;
import java.util.List;

public record DailyAvailabilityResponse(
        DayOfWeek dayOfWeek,
        Boolean enabled,
        List<TimeRangeResponse> timeRanges
) {
}