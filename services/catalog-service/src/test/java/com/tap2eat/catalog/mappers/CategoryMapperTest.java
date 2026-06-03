package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.DailyAvailabilityRequest;
import com.tap2eat.catalog.dtos.request.product.TimeRangeRequest;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void toDocument_shouldMapCreateRequestAndDefaults() {
        assertThat(mapper.toDocument(null)).isNull();

        CategoryDocument document = mapper.toDocument(CatalogTestDataFactory.createCategoryRequest());

        assertThat(document.getRestaurantId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(document.getName()).isEqualTo("Tacos");
        assertThat(document.getDisplayOrder()).isEqualTo(1);
        assertThat(document.getImage().getObjectKey()).isEqualTo("tap2eat/tests/request");
        assertThat(document.getIsActive()).isTrue();
    }

    @Test
    void updateDocument_shouldMapMutableFieldsAndIgnoreNulls() {
        CategoryDocument document = mapper.toDocument(CatalogTestDataFactory.createCategoryRequest());

        mapper.updateDocument(document, CatalogTestDataFactory.updateCategoryRequest());
        mapper.updateDocument(null, CatalogTestDataFactory.updateCategoryRequest());
        mapper.updateDocument(document, null);

        assertThat(document.getName()).isEqualTo("Bebidas");
        assertThat(document.getDescription()).isEqualTo("Drinks");
        assertThat(document.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void mapper_shouldHandleNullOptionalFieldsAndKeepDisplayOrderWhenUpdateOrderIsNull() {
        CategoryDocument document = mapper.toDocument(new CreateCategoryRequest(
                "restaurant-1",
                "Category",
                null,
                null,
                null,
                null
        ));

        assertThat(document.getDisplayOrder()).isZero();
        assertThat(document.getImage()).isNull();
        assertThat(document.getAvailability()).isNull();

        document.setDisplayOrder(5);
        mapper.updateDocument(document, new UpdateCategoryRequest(
                "Updated",
                null,
                null,
                null,
                null
        ));

        assertThat(document.getDisplayOrder()).isEqualTo(5);
        assertThat(document.getImage()).isNull();
        assertThat(document.getAvailability()).isNull();
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

        CategoryDocument document = mapper.toDocument(new CreateCategoryRequest(
                "restaurant-1",
                "Category",
                null,
                null,
                null,
                availability
        ));

        assertThat(document.getAvailability().getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(document.getAvailability().getWeeklySchedule()).hasSize(3);
        assertThat(document.getAvailability().getWeeklySchedule().get(0)).isNull();
        assertThat(document.getAvailability().getWeeklySchedule().get(1).getEnabled()).isFalse();
        assertThat(document.getAvailability().getWeeklySchedule().get(1).getTimeRanges().get(0)).isNull();
        assertThat(document.getAvailability().getWeeklySchedule().get(2).getTimeRanges()).isEmpty();
    }
}
