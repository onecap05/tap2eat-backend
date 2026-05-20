package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityEvaluatorImplTest {

    private AvailabilityEvaluatorImpl evaluator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-05-20T00:00:00Z"),
                ZoneId.of("America/Mexico_City")
        );
        evaluator = new AvailabilityEvaluatorImpl(clock);
    }

    @Test
    void branchWithoutAvailability_shouldBeOpenForCompatibility() {
        BranchDocument branch = CatalogTestDataFactory.branch("branch-1", "restaurant-1", null);

        assertThat(evaluator.isBranchOpen(branch)).isTrue();
    }

    @Test
    void branchWithNullWeeklySchedule_shouldBeOpenForCompatibility() {
        BranchDocument branch = CatalogTestDataFactory.branch();
        branch.getAvailability().setWeeklySchedule(null);

        assertThat(evaluator.isBranchOpen(branch)).isTrue();
    }

    @Test
    void branchWithEmptyWeeklySchedule_shouldBeOpenForCompatibility() {
        BranchDocument branch = CatalogTestDataFactory.branch();
        branch.getAvailability().setWeeklySchedule(List.of());

        assertThat(evaluator.isBranchOpen(branch)).isTrue();
    }

    @Test
    void branch_shouldBeOpenInsideConfiguredSchedule() {
        BranchDocument branch = CatalogTestDataFactory.branch();

        assertThat(evaluator.isBranchOpen(branch)).isTrue();
    }

    @Test
    void branch_shouldBeClosedOutsideConfiguredSchedule() {
        BranchDocument branch = CatalogTestDataFactory.branch(
                "branch-1",
                "restaurant-1",
                CatalogTestDataFactory.closedAvailability()
        );

        assertThat(evaluator.isBranchOpen(branch)).isFalse();
    }

    @Test
    void branch_shouldBeClosedWhenInactiveDeletedOrNull() {
        BranchDocument inactive = CatalogTestDataFactory.branch();
        inactive.setIsActive(Boolean.FALSE);
        BranchDocument deleted = CatalogTestDataFactory.branch();
        deleted.setDeletedAt(LocalDateTime.now());

        assertThat(evaluator.isBranchOpen(null)).isFalse();
        assertThat(evaluator.isBranchOpen(inactive)).isFalse();
        assertThat(evaluator.isBranchOpen(deleted)).isFalse();
    }

    @Test
    void productWithoutAvailability_shouldBeAvailable() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(null);

        assertThat(evaluator.isProductAvailable(product)).isTrue();
    }

    @Test
    void productWithNullOrEmptyWeeklySchedule_shouldBeAvailable() {
        ProductDocument nullSchedule = CatalogTestDataFactory.simpleProduct();
        nullSchedule.getAvailability().setWeeklySchedule(null);
        ProductDocument emptySchedule = CatalogTestDataFactory.simpleProduct();
        emptySchedule.getAvailability().setWeeklySchedule(List.of());

        assertThat(evaluator.isProductAvailable(nullSchedule)).isTrue();
        assertThat(evaluator.isProductAvailable(emptySchedule)).isTrue();
    }

    @Test
    void product_shouldRespectAvailabilityStatusAndSchedule() {
        ProductDocument available = CatalogTestDataFactory.simpleProduct();
        ProductDocument temporarilyUnavailable = CatalogTestDataFactory.simpleProduct();
        temporarilyUnavailable.getAvailability().setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        ProductDocument permanentlyUnavailable = CatalogTestDataFactory.simpleProduct();
        permanentlyUnavailable.getAvailability().setStatus(AvailabilityStatus.PERMANENTLY_UNAVAILABLE);
        ProductDocument outsideSchedule = CatalogTestDataFactory.simpleProduct();
        outsideSchedule.setAvailability(CatalogTestDataFactory.closedAvailability());

        assertThat(evaluator.isProductAvailable(available)).isTrue();
        assertThat(evaluator.isProductAvailable(temporarilyUnavailable)).isFalse();
        assertThat(evaluator.isProductAvailable(permanentlyUnavailable)).isFalse();
        assertThat(evaluator.isProductAvailable(outsideSchedule)).isFalse();
    }

    @Test
    void product_shouldBeUnavailableWhenInactiveDeletedOrNull() {
        ProductDocument inactive = CatalogTestDataFactory.simpleProduct();
        inactive.setIsActive(Boolean.FALSE);
        ProductDocument deleted = CatalogTestDataFactory.simpleProduct();
        deleted.setDeletedAt(LocalDateTime.now());

        assertThat(evaluator.isProductAvailable(null)).isFalse();
        assertThat(evaluator.isProductAvailable(inactive)).isFalse();
        assertThat(evaluator.isProductAvailable(deleted)).isFalse();
    }

    @Test
    void categoryWithoutAvailabilityOrSchedule_shouldBeAvailable() {
        CategoryDocument noAvailability = CatalogTestDataFactory.category();
        noAvailability.setAvailability(null);
        CategoryDocument nullSchedule = CatalogTestDataFactory.category();
        nullSchedule.getAvailability().setWeeklySchedule(null);
        CategoryDocument emptySchedule = CatalogTestDataFactory.category();
        emptySchedule.getAvailability().setWeeklySchedule(List.of());

        assertThat(evaluator.isCategoryAvailable(noAvailability)).isTrue();
        assertThat(evaluator.isCategoryAvailable(nullSchedule)).isTrue();
        assertThat(evaluator.isCategoryAvailable(emptySchedule)).isTrue();
    }

    @Test
    void category_shouldRespectScheduleAndVisibility() {
        CategoryDocument available = CatalogTestDataFactory.category();
        CategoryDocument outsideSchedule = CatalogTestDataFactory.category(
                "category-2",
                "restaurant-1",
                CatalogTestDataFactory.closedAvailability()
        );
        CategoryDocument inactive = CatalogTestDataFactory.category();
        inactive.setIsActive(Boolean.FALSE);

        assertThat(evaluator.isCategoryAvailable(available)).isTrue();
        assertThat(evaluator.isCategoryAvailable(outsideSchedule)).isFalse();
        assertThat(evaluator.isCategoryAvailable(inactive)).isFalse();
    }

    @Test
    void overnightSchedule_shouldBeAvailableAfterMidnightFromPreviousDay() {
        Clock overnightClock = Clock.fixed(
                Instant.parse("2026-05-20T08:00:00Z"),
                ZoneId.of("America/Mexico_City")
        );
        AvailabilityEvaluatorImpl overnightEvaluator = new AvailabilityEvaluatorImpl(overnightClock);
        AvailabilityConfig overnight = CatalogTestDataFactory.availability(
                AvailabilityStatus.AVAILABLE,
                DayOfWeek.TUESDAY,
                "22:00",
                "03:00"
        );
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(overnight);

        assertThat(overnightEvaluator.isProductAvailable(product)).isTrue();
    }

    @Test
    void scheduleEvaluation_shouldIgnoreInvalidDisabledOrWrongDayEntries() {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availability.setWeeklySchedule(Arrays.asList(
                null,
                new com.tap2eat.catalog.models.embedded.DailyAvailability(DayOfWeek.MONDAY, Boolean.TRUE, List.of(CatalogTestDataFactory.timeRange("10:00", "23:00"))),
                new com.tap2eat.catalog.models.embedded.DailyAvailability(DayOfWeek.TUESDAY, Boolean.FALSE, List.of(CatalogTestDataFactory.timeRange("10:00", "23:00"))),
                new com.tap2eat.catalog.models.embedded.DailyAvailability(DayOfWeek.TUESDAY, Boolean.TRUE, Arrays.asList(null, new com.tap2eat.catalog.models.embedded.TimeRange(null, java.time.LocalTime.NOON)))
        ));
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(availability);

        assertThat(evaluator.isProductAvailable(product)).isFalse();
    }

    @Test
    void scheduleEvaluation_shouldTreatEqualStartAndEndAsAllDay() {
        AvailabilityConfig availability = CatalogTestDataFactory.availability(
                AvailabilityStatus.AVAILABLE,
                DayOfWeek.TUESDAY,
                "10:00",
                "10:00"
        );
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(availability);

        assertThat(evaluator.isProductAvailable(product)).isTrue();
    }

    @Test
    void overnightSchedule_shouldBeAvailableOnCurrentDayBeforeMidnight() {
        AvailabilityConfig overnight = CatalogTestDataFactory.availability(
                AvailabilityStatus.AVAILABLE,
                DayOfWeek.TUESDAY,
                "17:00",
                "03:00"
        );
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(overnight);

        assertThat(evaluator.isProductAvailable(product)).isTrue();
    }
}
