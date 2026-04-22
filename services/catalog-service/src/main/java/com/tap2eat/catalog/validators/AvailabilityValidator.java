package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AvailabilityValidator {

    private AvailabilityValidator() {
    }

    public static void validate(AvailabilityConfig availabilityConfig) {
        if (availabilityConfig == null) {
            return;
        }

        List<DailyAvailability> weeklySchedule = availabilityConfig.getWeeklySchedule();
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            return;
        }

        validateDailyAvailabilities(weeklySchedule);
    }

    private static void validateDailyAvailabilities(List<DailyAvailability> weeklySchedule) {
        Set<DayOfWeek> processedDays = new HashSet<>();

        for (DailyAvailability dailyAvailability : weeklySchedule) {
            validateDailyAvailability(dailyAvailability, processedDays);
        }
    }

    private static void validateDailyAvailability(
            DailyAvailability dailyAvailability,
            Set<DayOfWeek> processedDays
    ) {
        if (dailyAvailability == null || dailyAvailability.getDayOfWeek() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        if (!processedDays.add(dailyAvailability.getDayOfWeek())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        List<TimeRange> timeRanges = dailyAvailability.getTimeRanges();
        if (timeRanges == null || timeRanges.isEmpty()) {
            return;
        }

        for (TimeRange timeRange : timeRanges) {
            validateTimeRange(timeRange);
        }
    }

    private static void validateTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_TIME_RANGE);
        }

        LocalTime startTime = timeRange.getStartTime();
        LocalTime endTime = timeRange.getEndTime();

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_TIME_RANGE);
        }
    }
}