package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;

import java.util.regex.Pattern;

public final class RestaurantValidator {

    private static final Pattern RFC_PATTERN = Pattern.compile("^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$");

    private RestaurantValidator() {
    }

    public static void validate(RestaurantDocument restaurant) {
        if (restaurant == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        validateRequiredFields(restaurant);
        validateRfc(restaurant.getRfc());
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

    private static void validateRfc(String rfc) {
        if (rfc == null) {
            return;
        }

        if (!RFC_PATTERN.matcher(rfc).matches()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
