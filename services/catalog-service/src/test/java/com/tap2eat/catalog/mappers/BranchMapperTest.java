package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.DailyAvailabilityRequest;
import com.tap2eat.catalog.dtos.request.product.TimeRangeRequest;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

class BranchMapperTest {

    private final BranchMapper mapper = new BranchMapper();

    @Test
    void toDocument_shouldMapCreateRequestAndDefaults() {
        assertThat(mapper.toDocument(null)).isNull();

        BranchDocument document = mapper.toDocument(CatalogTestDataFactory.createBranchRequest());

        assertThat(document.getRestaurantId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(document.getFormattedAddress()).contains("Reforma");
        assertThat(document.getAvailability().getWeeklySchedule()).hasSize(1);
        assertThat(document.getIsMainBranch()).isFalse();
        assertThat(document.getIsActive()).isTrue();
    }

    @Test
    void updateDocument_shouldMapFieldsAndKeepMainBranchWhenNull() {
        BranchDocument document = mapper.toDocument(CatalogTestDataFactory.createBranchRequest());

        mapper.updateDocument(document, CatalogTestDataFactory.updateBranchRequest());
        mapper.updateDocument(null, CatalogTestDataFactory.updateBranchRequest());
        mapper.updateDocument(document, null);

        assertThat(document.getName()).isEqualTo("Roma");
        assertThat(document.getPostalCode()).isEqualTo("06700");
        assertThat(document.getAvailability().getWeeklySchedule()).hasSize(1);
        assertThat(document.getIsMainBranch()).isTrue();
    }

    @Test
    void mapper_shouldHandleNullOptionalAvailabilityAndMainBranchValues() {
        CreateBranchRequest createRequest = new CreateBranchRequest(
                "restaurant-1", "Centro", null, "Address", null, null, null,
                "Centro", "Ciudad de Mexico", "CDMX", "06000", "Mexico", null,
                19.0, -99.0, null, null, null
        );
        BranchDocument document = mapper.toDocument(createRequest);

        assertThat(document.getAvailability()).isNull();
        assertThat(document.getIsMainBranch()).isFalse();

        document.setIsMainBranch(Boolean.TRUE);
        mapper.updateDocument(document, new UpdateBranchRequest(
                "Centro", null, "Address", null, null, null, "Centro",
                "Ciudad de Mexico", "CDMX", "06000", "Mexico", null,
                19.0, -99.0, null, null, null
        ));

        assertThat(document.getAvailability()).isNull();
        assertThat(document.getIsMainBranch()).isTrue();
    }

    @Test
    void mapper_shouldApplyAvailabilityDefaultsAndMapNullNestedItems() {
        AvailabilityConfigRequest availability = new AvailabilityConfigRequest(
                null,
                null,
                null,
                Arrays.asList(
                        null,
                        new DailyAvailabilityRequest(
                                DayOfWeek.MONDAY,
                                null,
                                Arrays.asList(null, new TimeRangeRequest(LocalTime.of(9, 0), LocalTime.of(10, 0)))
                        ),
                        new DailyAvailabilityRequest(DayOfWeek.TUESDAY, Boolean.TRUE, null)
                )
        );

        BranchDocument document = mapper.toDocument(new CreateBranchRequest(
                "restaurant-1", "Centro", null, "Address", null, null, null,
                "Centro", "Ciudad de Mexico", "CDMX", "06000", "Mexico", null,
                19.0, -99.0, null, availability, null
        ));

        assertThat(document.getAvailability().getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(document.getAvailability().getWeeklySchedule()).hasSize(3);
        assertThat(document.getAvailability().getWeeklySchedule().get(0)).isNull();
        assertThat(document.getAvailability().getWeeklySchedule().get(1).getEnabled()).isFalse();
        assertThat(document.getAvailability().getWeeklySchedule().get(1).getTimeRanges().get(0)).isNull();
        assertThat(document.getAvailability().getWeeklySchedule().get(2).getTimeRanges()).isEmpty();
    }
}
