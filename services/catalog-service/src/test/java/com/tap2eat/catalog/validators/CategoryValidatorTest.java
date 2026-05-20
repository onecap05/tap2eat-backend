package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryValidatorTest {

    @Test
    void validate_shouldAllowValidCategoryAndNullImage() {
        assertThatCode(() -> CategoryValidator.validate(CatalogTestDataFactory.category()))
                .doesNotThrowAnyException();
        CategoryDocument category = CatalogTestDataFactory.category();
        category.setImage(null);
        assertThatCode(() -> CategoryValidator.validate(category)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectMissingFieldsNegativeOrderAndInvalidImage() {
        assertInvalid(null);
        CategoryDocument missingRestaurant = CatalogTestDataFactory.category();
        missingRestaurant.setRestaurantId(" ");
        CategoryDocument missingName = CatalogTestDataFactory.category();
        missingName.setName(null);
        CategoryDocument negativeOrder = CatalogTestDataFactory.category();
        negativeOrder.setDisplayOrder(-1);
        CategoryDocument invalidImage = CatalogTestDataFactory.category();
        ImageMetadata image = CatalogTestDataFactory.imageMetadata();
        image.setProvider(null);
        invalidImage.setImage(image);

        assertInvalid(missingRestaurant);
        assertInvalid(missingName);
        assertInvalid(negativeOrder);
        assertInvalid(invalidImage);
    }

    private void assertInvalid(CategoryDocument category) {
        assertThatThrownBy(() -> CategoryValidator.validate(category))
                .isInstanceOf(CatalogValidationException.class);
    }
}
