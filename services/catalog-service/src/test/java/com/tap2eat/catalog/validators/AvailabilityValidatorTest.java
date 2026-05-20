package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityValidatorTest {

    @Test
    void validate_shouldAllowNullAvailabilityAndValidSchedule() {
        assertThatCode(() -> AvailabilityValidator.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> AvailabilityValidator.validate(CatalogTestDataFactory.openAvailability()))
                .doesNotThrowAnyException();
        AvailabilityConfig nullSchedule = CatalogTestDataFactory.openAvailability();
        nullSchedule.setWeeklySchedule(null);
        AvailabilityConfig emptySchedule = CatalogTestDataFactory.openAvailability();
        emptySchedule.setWeeklySchedule(List.of());
        AvailabilityConfig disabledDay = CatalogTestDataFactory.openAvailability();
        disabledDay.getWeeklySchedule().getFirst().setEnabled(Boolean.FALSE);
        disabledDay.getWeeklySchedule().getFirst().setTimeRanges(List.of());
        assertThatCode(() -> AvailabilityValidator.validate(nullSchedule)).doesNotThrowAnyException();
        assertThatCode(() -> AvailabilityValidator.validate(emptySchedule)).doesNotThrowAnyException();
        assertThatCode(() -> AvailabilityValidator.validate(disabledDay)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectInvalidStatusRules() {
        AvailabilityConfig missingStatus = CatalogTestDataFactory.openAvailability();
        missingStatus.setStatus(null);
        AvailabilityConfig availableWithReason = CatalogTestDataFactory.openAvailability();
        availableWithReason.setTemporaryReason(TemporaryUnavailabilityReason.OUT_OF_STOCK);
        AvailabilityConfig temporaryWithoutReason = CatalogTestDataFactory.openAvailability();
        temporaryWithoutReason.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        AvailabilityConfig permanentWithDetail = CatalogTestDataFactory.openAvailability();
        permanentWithDetail.setStatus(AvailabilityStatus.PERMANENTLY_UNAVAILABLE);
        permanentWithDetail.setTemporaryReasonDetail("legacy");

        assertInvalid(missingStatus);
        assertInvalid(availableWithReason);
        assertInvalid(temporaryWithoutReason);
        assertInvalid(permanentWithDetail);
    }

    @Test
    void validate_shouldAllowTemporaryUnavailableWithReason() {
        AvailabilityConfig availability = CatalogTestDataFactory.openAvailability();
        availability.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        availability.setTemporaryReason(TemporaryUnavailabilityReason.NO_SUPPLIES);

        assertThatCode(() -> AvailabilityValidator.validate(availability)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectInvalidDailyAvailability() {
        AvailabilityConfig missingDay = CatalogTestDataFactory.openAvailability();
        missingDay.setWeeklySchedule(List.of(new DailyAvailability(null, true, List.of(CatalogTestDataFactory.timeRange("10:00", "11:00")))));
        AvailabilityConfig duplicateDay = CatalogTestDataFactory.openAvailability();
        duplicateDay.setWeeklySchedule(List.of(
                CatalogTestDataFactory.dailyAvailability(DayOfWeek.TUESDAY, "10:00", "11:00"),
                CatalogTestDataFactory.dailyAvailability(DayOfWeek.TUESDAY, "12:00", "13:00")
        ));
        AvailabilityConfig nullEnabled = CatalogTestDataFactory.openAvailability();
        nullEnabled.getWeeklySchedule().getFirst().setEnabled(null);
        AvailabilityConfig disabledWithRange = CatalogTestDataFactory.openAvailability();
        disabledWithRange.getWeeklySchedule().getFirst().setEnabled(Boolean.FALSE);
        AvailabilityConfig enabledWithoutRange = CatalogTestDataFactory.openAvailability();
        enabledWithoutRange.getWeeklySchedule().getFirst().setTimeRanges(List.of());

        assertInvalid(missingDay);
        assertInvalid(duplicateDay);
        assertInvalid(nullEnabled);
        assertInvalid(disabledWithRange);
        assertInvalid(enabledWithoutRange);
    }

    @Test
    void validate_shouldRejectInvalidOrOverlappingTimeRanges() {
        AvailabilityConfig nullRange = CatalogTestDataFactory.openAvailability();
        nullRange.getWeeklySchedule().getFirst().setTimeRanges(Collections.singletonList((TimeRange) null));
        AvailabilityConfig nullTime = CatalogTestDataFactory.openAvailability();
        nullTime.getWeeklySchedule().getFirst().setTimeRanges(List.of(new TimeRange(null, LocalTime.NOON)));
        AvailabilityConfig reversed = CatalogTestDataFactory.openAvailability();
        reversed.getWeeklySchedule().getFirst().setTimeRanges(List.of(CatalogTestDataFactory.timeRange("12:00", "11:00")));
        AvailabilityConfig overlapping = CatalogTestDataFactory.openAvailability();
        overlapping.getWeeklySchedule().getFirst().setTimeRanges(List.of(
                CatalogTestDataFactory.timeRange("10:00", "12:00"),
                CatalogTestDataFactory.timeRange("11:00", "13:00")
        ));

        assertInvalid(nullRange);
        assertInvalid(nullTime);
        assertInvalid(reversed);
        assertInvalid(overlapping);
    }

    private void assertInvalid(AvailabilityConfig availability) {
        assertThatThrownBy(() -> AvailabilityValidator.validate(availability))
                .isInstanceOf(CatalogValidationException.class);
    }
}
