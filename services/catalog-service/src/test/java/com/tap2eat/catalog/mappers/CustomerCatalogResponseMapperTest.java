package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.response.customer.CustomerBranchResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerCategoryResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.ProductDocument;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCatalogResponseMapperTest {

    private final CustomerCatalogResponseMapper mapper = new CustomerCatalogResponseMapper();

    @Test
    void mapper_shouldReturnNullForNullDocuments() {
        assertThat(mapper.toRestaurantResponse(null, true)).isNull();
        assertThat(mapper.toBranchResponse(null, true)).isNull();
        assertThat(mapper.toCategoryResponse(null, true)).isNull();
        assertThat(mapper.toProductResponse(null, true)).isNull();
    }

    @Test
    void mapper_shouldMapCustomerResponsesWithAvailabilityFlags() {
        CustomerRestaurantResponse restaurant = mapper.toRestaurantResponse(CatalogTestDataFactory.restaurant(), true);
        CustomerBranchResponse branch = mapper.toBranchResponse(CatalogTestDataFactory.branch(), true);
        CustomerCategoryResponse category = mapper.toCategoryResponse(CatalogTestDataFactory.category(), true);
        ProductDocument productDocument = CatalogTestDataFactory.customizableProduct();
        CustomerProductResponse product = mapper.toProductResponse(productDocument, true);

        assertThat(restaurant.open()).isTrue();
        assertThat(restaurant.name()).isEqualTo("Demo Restaurant");
        assertThat(branch.open()).isTrue();
        assertThat(branch.availability().weeklySchedule()).hasSize(1);
        assertThat(category.available()).isTrue();
        assertThat(product.available()).isTrue();
        assertThat(product.modifierGroups()).hasSize(1);
        assertThat(product.image().objectKey()).isEqualTo("tap2eat/tests/image");
    }

    @Test
    void mapper_shouldReturnEmptyListsForNullNestedCollections() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setTags(null);
        product.setDietaryFlags(null);
        product.setAllergens(null);
        product.setModifierGroups(null);
        product.getAvailability().setWeeklySchedule(null);

        CustomerProductResponse response = mapper.toProductResponse(product, true);

        assertThat(response.tags()).isEmpty();
        assertThat(response.dietaryFlags()).isEmpty();
        assertThat(response.allergens()).isEmpty();
        assertThat(response.modifierGroups()).isEmpty();
        assertThat(response.availability().weeklySchedule()).isEmpty();
        assertThat(List.of(response)).hasSize(1);
    }

    @Test
    void mapper_shouldMapNullNestedItemsInCustomerCollections() {
        ProductDocument product = CatalogTestDataFactory.customizableProduct();
        product.getAvailability().setWeeklySchedule(Arrays.asList(null, product.getAvailability().getWeeklySchedule().getFirst()));
        product.getAvailability().getWeeklySchedule().get(1).setTimeRanges(Arrays.asList(null, CatalogTestDataFactory.timeRange("10:00", "11:00")));
        product.setModifierGroups(Arrays.asList(null, CatalogTestDataFactory.modifierGroup()));
        product.getModifierGroups().get(1).setOptions(Arrays.asList(null, CatalogTestDataFactory.modifierOption("option", "Option", true)));

        CustomerProductResponse response = mapper.toProductResponse(product, true);

        assertThat(response.availability().weeklySchedule().get(0)).isNull();
        assertThat(response.modifierGroups().get(0)).isNull();
        assertThat(response.modifierGroups().get(1).options().get(0)).isNull();
    }
}
