package com.tap2eat.catalog.dtos.request.product;

import java.time.DayOfWeek;
import java.util.List;

public record DailyAvailabilityRequest(
        DayOfWeek dayOfWeek,
        Boolean enabled,
        List<TimeRangeRequest> timeRanges
) {
}