package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.response.product.ProductResponse;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogResponseMapperTest {

    private final CatalogResponseMapper mapper = new CatalogResponseMapper();

    @Test
    void mapper_shouldReturnNullForNullDocumentsAndEmptyForNullLists() {
        assertThat(mapper.toRestaurantResponse(null)).isNull();
        assertThat(mapper.toBranchResponse(null)).isNull();
        assertThat(mapper.toCategoryResponse(null)).isNull();
        assertThat(mapper.toProductResponse(null)).isNull();
        assertThat(mapper.toRestaurantResponses(null)).isEmpty();
        assertThat(mapper.toBranchResponses(null)).isEmpty();
        assertThat(mapper.toCategoryResponses(null)).isEmpty();
        assertThat(mapper.toProductResponses(null)).isEmpty();
    }

    @Test
    void mapper_shouldMapDocumentsToOwnerResponses() {
        assertThat(mapper.toRestaurantResponse(CatalogTestDataFactory.restaurant()).ownerAccountId())
                .isEqualTo(CatalogTestDataFactory.OWNER_ID);
        assertThat(mapper.toBranchResponse(CatalogTestDataFactory.branch()).availability().weeklySchedule())
                .hasSize(1);
        assertThat(mapper.toCategoryResponse(CatalogTestDataFactory.category()).image().objectKey())
                .isEqualTo("tap2eat/tests/image");

        ProductResponse product = mapper.toProductResponse(CatalogTestDataFactory.customizableProduct());

        assertThat(product.modifierGroups()).hasSize(1);
        assertThat(product.tags()).containsExactly("popular");
    }

    @Test
    void mapper_shouldReturnEmptyListsForNullNestedProductCollections() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setTags(null);
        product.setDietaryFlags(null);
        product.setAllergens(null);
        product.setModifierGroups(null);
        product.getAvailability().setWeeklySchedule(null);

        ProductResponse response = mapper.toProductResponse(product);

        assertThat(response.tags()).isEmpty();
        assertThat(response.dietaryFlags()).isEmpty();
        assertThat(response.allergens()).isEmpty();
        assertThat(response.modifierGroups()).isEmpty();
        assertThat(response.availability().weeklySchedule()).isEmpty();
        assertThat(mapper.toProductResponses(List.of(product))).hasSize(1);
    }

    @Test
    void mapper_shouldMapNullNestedItemsInCollections() {
        ProductDocument product = CatalogTestDataFactory.customizableProduct();
        product.getAvailability().setWeeklySchedule(Arrays.asList(null, product.getAvailability().getWeeklySchedule().getFirst()));
        product.getAvailability().getWeeklySchedule().get(1).setTimeRanges(Arrays.asList(null, CatalogTestDataFactory.timeRange("10:00", "11:00")));
        product.setModifierGroups(Arrays.asList(null, CatalogTestDataFactory.modifierGroup()));
        product.getModifierGroups().get(1).setOptions(Arrays.asList(null, CatalogTestDataFactory.modifierOption("option", "Option", true)));

        BranchDocument branch = CatalogTestDataFactory.branch();
        branch.setAvailability(null);
        CategoryDocument category = CatalogTestDataFactory.category();
        category.setAvailability(null);
        category.setImage(null);

        assertThat(mapper.toProductResponse(product).availability().weeklySchedule().get(0)).isNull();
        assertThat(mapper.toProductResponse(product).modifierGroups().get(0)).isNull();
        assertThat(mapper.toBranchResponse(branch).availability()).isNull();
        assertThat(mapper.toCategoryResponse(category).image()).isNull();
    }
}
