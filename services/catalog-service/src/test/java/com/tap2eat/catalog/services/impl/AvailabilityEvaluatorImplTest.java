package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityEvaluatorImplTest {

    private AvailabilityEvaluatorImpl evaluator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-19T18:00:00Z"), ZoneId.of("UTC"));
        evaluator = new AvailabilityEvaluatorImpl(clock);
    }

    @Test
    void branch_shouldBeOpenInsideWeeklySchedule() {
        BranchDocument branch = branch(availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "17:00", "19:00"));

        assertThat(evaluator.isBranchOpen(branch)).isTrue();
    }

    @Test
    void branch_shouldBeClosedOutsideWeeklySchedule() {
        BranchDocument branch = branch(availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "08:00", "10:00"));

        assertThat(evaluator.isBranchOpen(branch)).isFalse();
    }

    @Test
    void branch_shouldBeClosedWhenScheduleIsMissing() {
        BranchDocument branch = branch(null);

        assertThat(evaluator.isBranchOpen(branch)).isFalse();
    }

    @Test
    void product_shouldBeAvailableWithAvailableStatusAndInsideSchedule() {
        ProductDocument product = product(availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "17:00", "19:00"));

        assertThat(evaluator.isProductAvailable(product)).isTrue();
    }

    @Test
    void product_shouldBeUnavailableWhenTemporarilyUnavailable() {
        ProductDocument product = product(availability(
                AvailabilityStatus.TEMPORARILY_UNAVAILABLE,
                DayOfWeek.TUESDAY,
                "17:00",
                "19:00"
        ));

        assertThat(evaluator.isProductAvailable(product)).isFalse();
    }

    @Test
    void product_shouldBeUnavailableWhenPermanentlyUnavailable() {
        ProductDocument product = product(availability(
                AvailabilityStatus.PERMANENTLY_UNAVAILABLE,
                DayOfWeek.TUESDAY,
                "17:00",
                "19:00"
        ));

        assertThat(evaluator.isProductAvailable(product)).isFalse();
    }

    @Test
    void product_shouldBeUnavailableOutsideWeeklySchedule() {
        ProductDocument product = product(availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "08:00", "10:00"));

        assertThat(evaluator.isProductAvailable(product)).isFalse();
    }

    @Test
    void category_shouldBeAvailableWithoutOwnSchedule() {
        CategoryDocument category = new CategoryDocument();
        category.setIsActive(Boolean.TRUE);

        assertThat(evaluator.isCategoryAvailable(category)).isTrue();
    }

    @Test
    void category_shouldBeUnavailableWhenInactive() {
        CategoryDocument category = new CategoryDocument();
        category.setIsActive(Boolean.FALSE);

        assertThat(evaluator.isCategoryAvailable(category)).isFalse();
    }

    @Test
    void category_shouldBeUnavailableOutsideOwnSchedule() {
        CategoryDocument category = new CategoryDocument();
        category.setIsActive(Boolean.TRUE);
        category.setAvailability(availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "08:00", "10:00"));

        assertThat(evaluator.isCategoryAvailable(category)).isFalse();
    }

    private BranchDocument branch(AvailabilityConfig availability) {
        BranchDocument branch = new BranchDocument();
        branch.setIsActive(Boolean.TRUE);
        branch.setAvailability(availability);
        return branch;
    }

    private ProductDocument product(AvailabilityConfig availability) {
        ProductDocument product = new ProductDocument();
        product.setIsActive(Boolean.TRUE);
        product.setAvailability(availability);
        return product;
    }

    private AvailabilityConfig availability(
            AvailabilityStatus status,
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime
    ) {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(status);
        availability.setWeeklySchedule(List.of(new DailyAvailability(
                dayOfWeek,
                Boolean.TRUE,
                List.of(new TimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime)))
        )));
        return availability;
    }
}
