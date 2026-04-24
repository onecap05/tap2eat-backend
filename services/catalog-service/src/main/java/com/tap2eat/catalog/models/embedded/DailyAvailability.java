package com.tap2eat.catalog.models.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAvailability {

    private DayOfWeek dayOfWeek;
    private Boolean enabled = true;

    @Builder.Default
    private List<TimeRange> timeRanges = new ArrayList<>();
}