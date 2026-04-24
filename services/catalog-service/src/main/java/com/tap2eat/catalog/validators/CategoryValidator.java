package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;

public final class CategoryValidator {

    private CategoryValidator() {
    }

    public static void validate(CategoryDocument category) {
        if (category == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        validateRequiredFields(category);
        validateDisplayOrder(category);
        validateImage(category.getImage());

        AvailabilityValidator.validate(category.getAvailability());
    }

    private static void validateRequiredFields(CategoryDocument category) {
        if (isBlank(category.getRestaurantId())
                || isBlank(category.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private static void validateDisplayOrder(CategoryDocument category) {
        Integer displayOrder = category.getDisplayOrder();
        if (displayOrder != null && displayOrder < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private static void validateImage(ImageMetadata image) {
        if (image == null) {
            return;
        }

        if (isBlank(image.getUrl())
                || isBlank(image.getObjectKey())
                || image.getProvider() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}