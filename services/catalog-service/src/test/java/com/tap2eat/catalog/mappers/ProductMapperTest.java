package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.ModifierGroupRequest;
import com.tap2eat.catalog.dtos.request.product.ModifierOptionRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.enums.ProductType;
import com.tap2eat.catalog.models.enums.SelectionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toDocument_shouldMapSimpleAndCustomizableRequests() {
        assertThat(mapper.toDocument(null)).isNull();

        ProductDocument simple = mapper.toDocument(CatalogTestDataFactory.createSimpleProductRequest());
        ProductDocument customizable = mapper.toDocument(CatalogTestDataFactory.createCustomizableProductRequest());

        assertThat(simple.getProductType()).isEqualTo(ProductType.SIMPLE);
        assertThat(simple.getTags()).containsExactly("popular");
        assertThat(customizable.getModifierGroups()).hasSize(1);
        assertThat(customizable.getModifierGroups().getFirst().getOptions()).hasSize(1);
        assertThat(customizable.getModifierGroups().getFirst().getId()).isEqualTo("group-1");
    }

    @Test
    void updateDocument_shouldMapMutableFieldsAndIgnoreNulls() {
        ProductDocument document = mapper.toDocument(CatalogTestDataFactory.createSimpleProductRequest());

        mapper.updateDocument(document, CatalogTestDataFactory.updateProductRequest());
        mapper.updateDocument(null, CatalogTestDataFactory.updateProductRequest());
        mapper.updateDocument(document, null);

        assertThat(document.getName()).isEqualTo("Updated taco");
        assertThat(document.getProductType()).isEqualTo(ProductType.CUSTOMIZABLE);
        assertThat(document.getModifierGroups()).hasSize(1);
        assertThat(document.getDisplayOrder()).isEqualTo(2);
        assertThat(document.getTags()).containsExactly("new");
    }

    @Test
    void toDocument_shouldApplyDefaultsForNullableCreateFieldsAndMapNullNestedItems() {
        ProductDocument document = mapper.toDocument(new CreateProductRequest(
                "restaurant-1",
                "category-1",
                "Defaulted",
                null,
                ProductType.SIMPLE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
        ProductDocument withNullNestedItems = mapper.toDocument(new CreateProductRequest(
                "restaurant-1",
                "category-1",
                "Nested",
                null,
                ProductType.CUSTOMIZABLE,
                BigDecimal.TEN,
                null,
                Arrays.asList((ModifierGroupRequest) null, new ModifierGroupRequest(
                        null,
                        "Extras",
                        SelectionType.MULTIPLE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Arrays.asList((ModifierOptionRequest) null, new ModifierOptionRequest(null, "Queso", null, null, null))
                )),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(document.getIsActive()).isTrue();
        assertThat(document.getDisplayOrder()).isZero();
        assertThat(document.getFeatured()).isFalse();
        assertThat(document.getTags()).isEmpty();
        assertThat(document.getModifierGroups()).isEmpty();
        assertThat(withNullNestedItems.getModifierGroups()).hasSize(2);
        assertThat(withNullNestedItems.getModifierGroups().get(0)).isNull();
        assertThat(withNullNestedItems.getModifierGroups().get(1).getId()).isNotBlank();
        assertThat(withNullNestedItems.getModifierGroups().get(1).getOptions().get(0)).isNull();
        assertThat(withNullNestedItems.getModifierGroups().get(1).getOptions().get(1).getAdditionalPrice()).isZero();
    }

    @Test
    void updateDocument_shouldKeepOptionalFieldsWhenRequestValuesAreNull() {
        ProductDocument document = mapper.toDocument(CatalogTestDataFactory.createSimpleProductRequest());
        document.setIsActive(Boolean.FALSE);
        document.setDisplayOrder(7);
        document.setFeatured(Boolean.TRUE);
        document.setTags(List.of("keep"));
        document.setDietaryFlags(List.of("diet"));
        document.setAllergens(List.of("allergen"));

        mapper.updateDocument(document, new UpdateProductRequest(
                "category-1",
                "Partial",
                null,
                ProductType.SIMPLE,
                BigDecimal.ONE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(document.getIsActive()).isFalse();
        assertThat(document.getDisplayOrder()).isEqualTo(7);
        assertThat(document.getFeatured()).isTrue();
        assertThat(document.getTags()).containsExactly("keep");
        assertThat(document.getDietaryFlags()).containsExactly("diet");
        assertThat(document.getAllergens()).containsExactly("allergen");
    }
}
