package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestaurantMapperTest {

    private final RestaurantMapper mapper = new RestaurantMapper();

    @Test
    void toDocument_shouldMapCreateRequestAndNull() {
        assertThat(mapper.toDocument(null)).isNull();

        RestaurantDocument document = mapper.toDocument(CatalogTestDataFactory.createRestaurantRequest());

        assertThat(document.getOwnerAccountId()).isEqualTo(CatalogTestDataFactory.OWNER_ID);
        assertThat(document.getName()).isEqualTo("Demo Restaurant");
        assertThat(document.getRfc()).isEqualTo(CatalogTestDataFactory.RESTAURANT_RFC);
        assertThat(document.getLogo().getObjectKey()).isEqualTo("tap2eat/tests/request");
        assertThat(document.getIsActive()).isTrue();
    }

    @Test
    void updateDocument_shouldMapMutableFieldsAndIgnoreNulls() {
        RestaurantDocument document = CatalogTestDataFactory.restaurant();

        mapper.updateDocument(document, CatalogTestDataFactory.updateRestaurantRequest());
        mapper.updateDocument(null, CatalogTestDataFactory.updateRestaurantRequest());
        mapper.updateDocument(document, null);

        assertThat(document.getName()).isEqualTo("Updated Restaurant");
        assertThat(document.getDescription()).isEqualTo("Updated description");
        assertThat(document.getRfc()).isEqualTo("UPD260520ABC");
        assertThat(document.getLogo().getUrl()).isEqualTo("https://cdn.tap2eat.test/request.webp");
    }
}
