package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import com.tap2eat.catalog.models.enums.ProductType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductValidatorTest {

    @Test
    void validate_shouldAllowValidSimpleAndCustomizableProducts() {
        assertThatCode(() -> ProductValidator.validate(CatalogTestDataFactory.simpleProduct()))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProductValidator.validate(CatalogTestDataFactory.customizableProduct()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectMissingRequiredFieldsAndNegativeValues() {
        assertInvalid(null);
        ProductDocument missingRestaurant = CatalogTestDataFactory.simpleProduct();
        missingRestaurant.setRestaurantId(" ");
        ProductDocument missingCategory = CatalogTestDataFactory.simpleProduct();
        missingCategory.setCategoryId(null);
        ProductDocument missingName = CatalogTestDataFactory.simpleProduct();
        missingName.setName(" ");
        ProductDocument missingType = CatalogTestDataFactory.simpleProduct();
        missingType.setProductType(null);
        ProductDocument negativePrice = CatalogTestDataFactory.simpleProduct();
        negativePrice.setPrice(BigDecimal.valueOf(-1));
        ProductDocument nullPrice = CatalogTestDataFactory.simpleProduct();
        nullPrice.setPrice(null);
        ProductDocument negativeOrder = CatalogTestDataFactory.simpleProduct();
        negativeOrder.setDisplayOrder(-1);

        assertInvalid(missingRestaurant);
        assertInvalid(missingCategory);
        assertInvalid(missingName);
        assertInvalid(missingType);
        assertInvalid(nullPrice);
        assertInvalid(negativePrice);
        assertInvalid(negativeOrder);
    }

    @Test
    void validate_shouldRejectInvalidImage() {
        ProductDocument missingImage = CatalogTestDataFactory.simpleProduct();
        missingImage.setImage(null);
        ProductDocument invalidImage = CatalogTestDataFactory.simpleProduct();
        ImageMetadata image = CatalogTestDataFactory.imageMetadata();
        image.setObjectKey(" ");
        invalidImage.setImage(image);
        ProductDocument invalidProvider = CatalogTestDataFactory.simpleProduct();
        ImageMetadata imageWithoutProvider = CatalogTestDataFactory.imageMetadata();
        imageWithoutProvider.setProvider(null);
        invalidProvider.setImage(imageWithoutProvider);

        assertInvalid(missingImage);
        assertInvalid(invalidImage);
        assertInvalid(invalidProvider);
    }

    @Test
    void validate_shouldRejectInvalidStringLists() {
        ProductDocument blankTag = CatalogTestDataFactory.simpleProduct();
        blankTag.setTags(List.of("popular", " "));
        ProductDocument duplicateAllergen = CatalogTestDataFactory.simpleProduct();
        duplicateAllergen.setAllergens(List.of("Soy", " soy "));

        assertInvalid(blankTag);
        assertInvalid(duplicateAllergen);
    }

    @Test
    void validate_shouldRejectSimpleProductWithModifierGroupsAndCustomizableWithoutActiveGroups() {
        ProductDocument simpleWithModifiers = CatalogTestDataFactory.simpleProduct();
        simpleWithModifiers.setModifierGroups(List.of(CatalogTestDataFactory.modifierGroup()));
        ProductDocument customizableWithoutGroups = CatalogTestDataFactory.simpleProduct();
        customizableWithoutGroups.setProductType(ProductType.CUSTOMIZABLE);
        customizableWithoutGroups.setModifierGroups(List.of());

        assertInvalid(simpleWithModifiers);
        assertInvalid(customizableWithoutGroups);
    }

    private void assertInvalid(ProductDocument product) {
        assertThatThrownBy(() -> ProductValidator.validate(product))
                .isInstanceOf(CatalogValidationException.class);
    }
}
