package com.tap2eat.catalog.models.embedded;

import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityConfig {

    private AvailabilityStatus status = AvailabilityStatus.ACTIVE;

    @Builder.Default
    private List<DailyAvailability> weeklySchedule = new ArrayList<>();
}