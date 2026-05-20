package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.services.IAvailabilityEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityEvaluatorImpl implements IAvailabilityEvaluator {

    private final Clock clock;

    @Override
    public boolean isBranchOpen(BranchDocument branch) {
        return branch != null
                && isVisible(branch.getIsActive(), branch.getDeletedAt() == null)
                && isAvailableNow(branch.getAvailability(), true);
    }

    @Override
    public boolean isCategoryAvailable(CategoryDocument category) {
        return category != null
                && isVisible(category.getIsActive(), category.getDeletedAt() == null)
                && isAvailableNow(category.getAvailability(), true);
    }

    @Override
    public boolean isProductAvailable(ProductDocument product) {
        return product != null
                && isVisible(product.getIsActive(), product.getDeletedAt() == null)
                && isAvailableNow(product.getAvailability(), true);
    }

    @Override
    public boolean isAvailableNow(AvailabilityConfig availability, boolean emptyScheduleAvailable) {
        if (availability == null) {
            return emptyScheduleAvailable;
        }

        AvailabilityStatus status = availability.getStatus();

        if (status != null && !AvailabilityStatus.AVAILABLE.equals(status)) {
            return false;
        }

        return isWithinWeeklySchedule(availability.getWeeklySchedule(), emptyScheduleAvailable);
    }

    private boolean isWithinWeeklySchedule(
            List<DailyAvailability> weeklySchedule,
            boolean emptyScheduleAvailable
    ) {
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            return emptyScheduleAvailable;
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        DayOfWeek currentDay = today.getDayOfWeek();
        DayOfWeek previousDay = today.minusDays(1).getDayOfWeek();

        return weeklySchedule.stream()
                .anyMatch(day -> isOpenForCurrentDay(day, currentDay, now)
                        || isOpenFromPreviousDay(day, previousDay, now));
    }

    private boolean isOpenForCurrentDay(
            DailyAvailability day,
            DayOfWeek currentDay,
            LocalTime now
    ) {
        if (!isEnabledDay(day, currentDay)) {
            return false;
        }

        return safeRanges(day).stream()
                .anyMatch(range -> isInsideCurrentDayRange(range, now));
    }

    private boolean isOpenFromPreviousDay(
            DailyAvailability day,
            DayOfWeek previousDay,
            LocalTime now
    ) {
        if (!isEnabledDay(day, previousDay)) {
            return false;
        }

        return safeRanges(day).stream()
                .anyMatch(range -> isInsidePreviousOvernightRange(range, now));
    }

    private boolean isInsideCurrentDayRange(TimeRange range, LocalTime now) {
        if (!isValidRange(range)) {
            return false;
        }

        LocalTime start = range.getStartTime();
        LocalTime end = range.getEndTime();

        if (start.equals(end)) {
            return true;
        }

        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }

        return !now.isBefore(start);
    }

    private boolean isInsidePreviousOvernightRange(TimeRange range, LocalTime now) {
        if (!isValidRange(range)) {
            return false;
        }

        LocalTime start = range.getStartTime();
        LocalTime end = range.getEndTime();

        return start.isAfter(end) && now.isBefore(end);
    }

    private boolean isEnabledDay(DailyAvailability day, DayOfWeek expectedDay) {
        return day != null
                && expectedDay.equals(day.getDayOfWeek())
                && Boolean.TRUE.equals(day.getEnabled());
    }

    private List<TimeRange> safeRanges(DailyAvailability day) {
        if (day == null || day.getTimeRanges() == null) {
            return List.of();
        }

        return day.getTimeRanges();
    }

    private boolean isValidRange(TimeRange range) {
        return range != null
                && range.getStartTime() != null
                && range.getEndTime() != null;
    }

    private boolean isVisible(Boolean active, boolean notDeleted) {
        return Boolean.TRUE.equals(active) && notDeleted;
    }
}