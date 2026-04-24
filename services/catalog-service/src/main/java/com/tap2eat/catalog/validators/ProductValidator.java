package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProductValidator {

    private ProductValidator() {
    }

    public static void validate(ProductDocument product) {
        if (product == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        validateRequiredFields(product);
        validateDisplayOrder(product);
        validateImage(product.getImage());
        validateStringList(product.getTags());
        validateStringList(product.getDietaryFlags());
        validateStringList(product.getAllergens());

        AvailabilityValidator.validate(product.getAvailability());
        ModifierGroupValidator.validateAll(product.getModifierGroups());
    }

    private static void validateRequiredFields(ProductDocument product) {
        if (isBlank(product.getRestaurantId())
                || isBlank(product.getCategoryId())
                || isBlank(product.getName())
                || product.getProductType() == null
                || product.getPrice() == null
                || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static void validateDisplayOrder(ProductDocument product) {
        Integer displayOrder = product.getDisplayOrder();
        if (displayOrder != null && displayOrder < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static void validateImage(ImageMetadata image) {
        if (image == null
                || isBlank(image.getUrl())
                || isBlank(image.getObjectKey())
                || image.getProvider() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static void validateStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        Set<String> normalizedValues = new HashSet<>();

        for (String value : values) {
            if (isBlank(value)) {
                throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
            }

            String normalized = normalize(value);
            if (!normalizedValues.add(normalized)) {
                throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }
}