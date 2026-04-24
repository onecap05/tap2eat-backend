package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;

public final class RestaurantValidator {

    private RestaurantValidator() {
    }

    public static void validate(RestaurantDocument restaurant) {
        if (restaurant == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        validateRequiredFields(restaurant);
        validateLogo(restaurant.getLogo());
    }

    private static void validateRequiredFields(RestaurantDocument restaurant) {
        if (isBlank(restaurant.getOwnerAccountId()) || isBlank(restaurant.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }
    }

    private static void validateLogo(ImageMetadata logo) {
        if (logo == null) {
            return;
        }

        if (isBlank(logo.getUrl())
                || isBlank(logo.getObjectKey())
                || logo.getProvider() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}