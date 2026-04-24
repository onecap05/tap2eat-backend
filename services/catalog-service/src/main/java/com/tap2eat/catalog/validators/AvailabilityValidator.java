package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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

        validateStatusRules(availabilityConfig);

        List<DailyAvailability> weeklySchedule = availabilityConfig.getWeeklySchedule();
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            return;
        }

        validateDailyAvailabilities(weeklySchedule);
    }

    private static void validateStatusRules(AvailabilityConfig availabilityConfig) {
        AvailabilityStatus status = availabilityConfig.getStatus();

        if (status == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        switch (status) {
            case AVAILABLE -> {
                if (availabilityConfig.getTemporaryReason() != null
                        || hasText(availabilityConfig.getTemporaryReasonDetail())) {
                    throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
                }
            }
            case TEMPORARILY_UNAVAILABLE -> {
                if (availabilityConfig.getTemporaryReason() == null) {
                    throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
                }
            }
            case PERMANENTLY_UNAVAILABLE -> {
                if (availabilityConfig.getTemporaryReason() != null
                        || hasText(availabilityConfig.getTemporaryReasonDetail())) {
                    throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
                }
            }
            default -> throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }
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

        Boolean enabled = dailyAvailability.getEnabled();
        List<TimeRange> timeRanges = dailyAvailability.getTimeRanges();

        if (enabled == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        if (Boolean.FALSE.equals(enabled)) {
            if (timeRanges != null && !timeRanges.isEmpty()) {
                throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
            }
            return;
        }

        if (timeRanges == null || timeRanges.isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        validateTimeRanges(timeRanges);
    }

    private static void validateTimeRanges(List<TimeRange> timeRanges) {
        List<TimeRange> sortedRanges = new ArrayList<>();

        for (TimeRange timeRange : timeRanges) {
            validateTimeRange(timeRange);
            sortedRanges.add(timeRange);
        }

        sortedRanges.sort(Comparator.comparing(TimeRange::getStartTime));

        for (int i = 1; i < sortedRanges.size(); i++) {
            TimeRange previous = sortedRanges.get(i - 1);
            TimeRange current = sortedRanges.get(i);

            if (!current.getStartTime().isAfter(previous.getEndTime())) {
                throw new CatalogValidationException(CatalogErrorCode.INVALID_TIME_RANGE);
            }
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}