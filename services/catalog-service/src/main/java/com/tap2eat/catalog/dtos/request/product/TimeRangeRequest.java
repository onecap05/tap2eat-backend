package com.tap2eat.catalog.dtos.request.product;

import java.time.LocalTime;

public record TimeRangeRequest(
        LocalTime startTime,
        LocalTime endTime
) {
}