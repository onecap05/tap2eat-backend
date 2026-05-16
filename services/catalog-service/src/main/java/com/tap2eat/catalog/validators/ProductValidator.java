package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.enums.ProductType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProductValidator {

    private ProductValidator() {
    }

    public static void validate(ProductDocument product) {
        validateProductExists(product);
        validateRequiredFields(product);
        validateDisplayOrder(product);
        validateImage(product.getImage());
        validateStringList(product.getTags());
        validateStringList(product.getDietaryFlags());
        validateStringList(product.getAllergens());
        validateProductModifierGroups(product);

        AvailabilityValidator.validate(product.getAvailability());
    }

    private static void validateProductExists(ProductDocument product) {
        if (product == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
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

    private static void validateProductModifierGroups(ProductDocument product) {
        if (product.getProductType() == ProductType.SIMPLE) {
            validateSimpleProductModifierGroups(product.getModifierGroups());
            return;
        }

        validateCustomizableProductModifierGroups(product.getModifierGroups());
    }

    private static void validateSimpleProductModifierGroups(List<ModifierGroup> groups) {
        if (groups != null && !groups.isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static void validateCustomizableProductModifierGroups(List<ModifierGroup> groups) {
        if (!hasActiveModifierGroup(groups)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        ModifierGroupValidator.validateAll(groups);
    }

    private static boolean hasActiveModifierGroup(List<ModifierGroup> groups) {
        return groups != null && groups.stream().anyMatch(ProductValidator::isActiveModifierGroup);
    }

    private static boolean isActiveModifierGroup(ModifierGroup group) {
        return group != null && !Boolean.FALSE.equals(group.getIsActive());
    }

    private static void validateStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        Set<String> normalizedValues = new HashSet<>();

        for (String value : values) {
            validateStringValue(value);
            validateUniqueValue(value, normalizedValues);
        }
    }

    private static void validateStringValue(String value) {
        if (isBlank(value)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static void validateUniqueValue(String value, Set<String> normalizedValues) {
        String normalized = normalize(value);

        if (!normalizedValues.add(normalized)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }
}