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

    private static final int MIN_SELECTIONS = 0;
    private static final int SINGLE_SELECTION_LIMIT = 1;
    private static final int REQUIRED_SINGLE_SELECTIONS = 1;
    private static final int OPTIONAL_SINGLE_SELECTIONS = 0;

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
        validateGroupExists(group);
        validateGroupName(group);
        validateSelectionRules(group);
        validateDisplayOrder(group);
        validateOptions(group);
    }

    private static void validateGroupExists(ModifierGroup group) {
        if (group == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateGroupName(ModifierGroup group) {
        if (isBlank(group.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateSelectionRules(ModifierGroup group) {
        validateSelectionValues(group);
        validateSelectionRange(group);
        validateSelectionTypeRules(group);
    }

    private static void validateSelectionValues(ModifierGroup group) {
        if (group.getSelectionType() == null
                || group.getMinSelections() == null
                || group.getMaxSelections() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateSelectionRange(ModifierGroup group) {
        Integer minSelections = group.getMinSelections();
        Integer maxSelections = group.getMaxSelections();

        if (minSelections < MIN_SELECTIONS
                || maxSelections < MIN_SELECTIONS
                || minSelections > maxSelections) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateSelectionTypeRules(ModifierGroup group) {
        if (group.getSelectionType() == SelectionType.SINGLE) {
            validateSingleSelectionRules(group);
            return;
        }

        validateMultipleSelectionRules(group);
    }

    private static void validateSingleSelectionRules(ModifierGroup group) {
        if (group.getMaxSelections() != SINGLE_SELECTION_LIMIT) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }

        int expectedMinSelections = getExpectedSingleMinSelections(group);

        if (group.getMinSelections() != expectedMinSelections) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static int getExpectedSingleMinSelections(ModifierGroup group) {
        return Boolean.TRUE.equals(group.getRequired())
                ? REQUIRED_SINGLE_SELECTIONS
                : OPTIONAL_SINGLE_SELECTIONS;
    }

    private static void validateMultipleSelectionRules(ModifierGroup group) {
        if (Boolean.TRUE.equals(group.getRequired()) && group.getMinSelections() < REQUIRED_SINGLE_SELECTIONS) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateDisplayOrder(ModifierGroup group) {
        Integer displayOrder = group.getDisplayOrder();

        if (displayOrder != null && displayOrder < MIN_SELECTIONS) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateOptions(ModifierGroup group) {
        List<ModifierOption> options = group.getOptions();

        validateActiveGroupHasOptions(group, options);

        if (options == null || options.isEmpty()) {
            return;
        }

        validateOptionValues(options);
        validateMaxSelectionsAgainstActiveOptions(group, options);
    }

    private static void validateActiveGroupHasOptions(ModifierGroup group, List<ModifierOption> options) {
        if (isActive(group) && (options == null || options.isEmpty())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static void validateOptionValues(List<ModifierOption> options) {
        Set<String> normalizedNames = new HashSet<>();

        for (ModifierOption option : options) {
            validateOption(option, normalizedNames);
        }
    }

    private static void validateMaxSelectionsAgainstActiveOptions(ModifierGroup group, List<ModifierOption> options) {
        if (!isActive(group)) {
            return;
        }

        int activeOptionCount = countActiveOptions(options);

        if (group.getMaxSelections() > activeOptionCount) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_GROUP);
        }
    }

    private static int countActiveOptions(List<ModifierOption> options) {
        return (int) options.stream()
                .filter(ModifierGroupValidator::isActiveOption)
                .count();
    }

    private static void validateOption(ModifierOption option, Set<String> normalizedNames) {
        validateOptionExists(option);
        validateOptionName(option, normalizedNames);
        validateOptionPrice(option);
        validateOptionDisplayOrder(option);
    }

    private static void validateOptionExists(ModifierOption option) {
        if (option == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }
    }

    private static void validateOptionName(ModifierOption option, Set<String> normalizedNames) {
        if (isBlank(option.getName())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }

        String normalizedName = normalize(option.getName());

        if (!normalizedNames.add(normalizedName)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }
    }

    private static void validateOptionPrice(ModifierOption option) {
        BigDecimal additionalPrice = option.getAdditionalPrice();

        if (additionalPrice == null || additionalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }
    }

    private static void validateOptionDisplayOrder(ModifierOption option) {
        Integer displayOrder = option.getDisplayOrder();

        if (displayOrder != null && displayOrder < MIN_SELECTIONS) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION);
        }
    }

    private static boolean isActive(ModifierGroup group) {
        return !Boolean.FALSE.equals(group.getIsActive());
    }

    private static boolean isActiveOption(ModifierOption option) {
        return option != null && !Boolean.FALSE.equals(option.getIsActive());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }
}