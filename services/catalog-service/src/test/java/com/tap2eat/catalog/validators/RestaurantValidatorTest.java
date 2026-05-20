package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestaurantValidatorTest {

    @Test
    void validate_shouldAllowValidRestaurantAndNullLogo() {
        assertThatCode(() -> RestaurantValidator.validate(CatalogTestDataFactory.restaurant()))
                .doesNotThrowAnyException();
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        restaurant.setLogo(null);
        assertThatCode(() -> RestaurantValidator.validate(restaurant)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectMissingRequiredFieldsAndInvalidLogo() {
        assertInvalid(null);
        RestaurantDocument missingOwner = CatalogTestDataFactory.restaurant();
        missingOwner.setOwnerAccountId(" ");
        RestaurantDocument missingName = CatalogTestDataFactory.restaurant();
        missingName.setName(null);
        RestaurantDocument invalidLogo = CatalogTestDataFactory.restaurant();
        ImageMetadata logo = CatalogTestDataFactory.imageMetadata();
        logo.setUrl(" ");
        invalidLogo.setLogo(logo);

        assertInvalid(missingOwner);
        assertInvalid(missingName);
        assertInvalid(invalidLogo);
    }

    private void assertInvalid(RestaurantDocument restaurant) {
        assertThatThrownBy(() -> RestaurantValidator.validate(restaurant))
                .isInstanceOf(CatalogValidationException.class);
    }
}
