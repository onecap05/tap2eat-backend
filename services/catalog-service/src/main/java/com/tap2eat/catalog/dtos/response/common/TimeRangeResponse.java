package com.tap2eat.catalog.dtos.response.common;

import java.time.LocalTime;

public record TimeRangeResponse(
        LocalTime startTime,
        LocalTime endTime
) {
}