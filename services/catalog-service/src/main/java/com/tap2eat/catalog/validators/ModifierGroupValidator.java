package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.models.enums.SelectionType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModifierGroupValidator {

    private ModifierGroupValidator() {
    }

    public static void validateAll(List<ModifierGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (ModifierGroup group : groups) {
            validate(group);
        }
    }

    public static void validate(ModifierGroup group) {
        if (group == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        validateGroupName(group);
        validateSelectionRules(group);
        validateDisplayOrder(group);
        validateOptions(group);
    }

    private static void validateGroupName(ModifierGroup group) {
        if (isBlank(group.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateSelectionRules(ModifierGroup group) {
        Integer minSelections = group.getMinSelections();
        Integer maxSelections = group.getMaxSelections();

        if (group.getSelectionType() == null
                || minSelections == null
                || maxSelections == null
                || minSelections < 0
                || maxSelections < 0
                || minSelections > maxSelections) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        if (group.getSelectionType() == SelectionType.SINGLE && maxSelections > 1) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        if (Boolean.TRUE.equals(group.getRequired()) && minSelections < 1) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateDisplayOrder(ModifierGroup group) {
        Integer displayOrder = group.getDisplayOrder();
        if (displayOrder != null && displayOrder < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateOptions(ModifierGroup group) {
        List<ModifierOption> options = group.getOptions();

        if (Boolean.TRUE.equals(group.getIsActive()) && (options == null || options.isEmpty())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        if (options == null || options.isEmpty()) {
            return;
        }

        Set<String> normalizedNames = new HashSet<>();

        for (ModifierOption option : options) {
            validateOption(option, normalizedNames);
        }
    }

    private static void validateOption(ModifierOption option, Set<String> normalizedNames) {
        if (option == null || isBlank(option.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }

        String normalizedName = normalize(option.getName());
        if (!normalizedNames.add(normalizedName)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }

        BigDecimal additionalPrice = option.getAdditionalPrice();
        if (additionalPrice == null || additionalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }

        Integer displayOrder = option.getDisplayOrder();
        if (displayOrder != null && displayOrder < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }
}