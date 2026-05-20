package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.models.enums.SelectionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModifierGroupValidatorTest {

    @Test
    void validateAll_shouldAllowNullEmptyAndValidGroups() {
        assertThatCode(() -> ModifierGroupValidator.validateAll(null)).doesNotThrowAnyException();
        assertThatCode(() -> ModifierGroupValidator.validateAll(List.of())).doesNotThrowAnyException();
        assertThatCode(() -> ModifierGroupValidator.validate(CatalogTestDataFactory.modifierGroup()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectInvalidGroupSelectionRules() {
        assertInvalid(null);
        assertInvalid(groupWithName(" "));
        assertInvalid(groupWithSelection(null, 0, 1, false));
        assertInvalid(groupWithSelection(SelectionType.MULTIPLE, -1, 1, false));
        assertInvalid(groupWithSelection(SelectionType.MULTIPLE, 2, 1, false));
        assertInvalid(groupWithSelection(SelectionType.MULTIPLE, 0, -1, false));
        assertInvalid(groupWithSelection(SelectionType.SINGLE, 0, 2, false));
        assertInvalid(groupWithSelection(SelectionType.SINGLE, 1, 1, false));
        assertInvalid(groupWithSelection(SelectionType.MULTIPLE, 0, 2, true));
    }

    @Test
    void validate_shouldRejectInvalidDisplayOrderAndOptions() {
        ModifierGroup negativeOrder = CatalogTestDataFactory.modifierGroup();
        negativeOrder.setDisplayOrder(-1);
        ModifierGroup activeWithoutOptions = CatalogTestDataFactory.modifierGroup();
        activeWithoutOptions.setOptions(List.of());
        ModifierGroup blankOption = CatalogTestDataFactory.modifierGroup();
        blankOption.setOptions(List.of(CatalogTestDataFactory.modifierOption("option-1", " ", true)));
        ModifierGroup duplicateOption = CatalogTestDataFactory.modifierGroup();
        duplicateOption.setOptions(List.of(
                CatalogTestDataFactory.modifierOption("option-1", "Verde", true),
                CatalogTestDataFactory.modifierOption("option-2", " verde ", true)
        ));
        ModifierGroup negativePrice = CatalogTestDataFactory.modifierGroup();
        ModifierOption option = CatalogTestDataFactory.modifierOption("option-1", "Verde", true);
        option.setAdditionalPrice(BigDecimal.valueOf(-1));
        negativePrice.setOptions(List.of(option));
        ModifierGroup nullOption = CatalogTestDataFactory.modifierGroup();
        nullOption.setOptions(java.util.Collections.singletonList(null));
        ModifierGroup negativeOptionDisplay = CatalogTestDataFactory.modifierGroup();
        ModifierOption negativeDisplay = CatalogTestDataFactory.modifierOption("option-1", "Verde", true);
        negativeDisplay.setDisplayOrder(-1);
        negativeOptionDisplay.setOptions(List.of(negativeDisplay));
        ModifierGroup maxGreaterThanActiveOptions = groupWithSelection(SelectionType.MULTIPLE, 0, 2, false);
        maxGreaterThanActiveOptions.setOptions(List.of(CatalogTestDataFactory.modifierOption("option-1", "Verde", true)));

        assertInvalid(negativeOrder);
        assertInvalid(activeWithoutOptions);
        assertInvalid(blankOption);
        assertInvalid(duplicateOption);
        assertInvalid(negativePrice);
        assertInvalid(nullOption);
        assertInvalid(negativeOptionDisplay);
        assertInvalid(maxGreaterThanActiveOptions);
    }

    @Test
    void validate_shouldAllowInactiveGroupWithoutOptions() {
        ModifierGroup group = CatalogTestDataFactory.modifierGroup();
        group.setIsActive(Boolean.FALSE);
        group.setOptions(List.of());

        assertThatCode(() -> ModifierGroupValidator.validate(group)).doesNotThrowAnyException();
    }

    private ModifierGroup groupWithName(String name) {
        ModifierGroup group = CatalogTestDataFactory.modifierGroup();
        group.setName(name);
        return group;
    }

    private ModifierGroup groupWithSelection(SelectionType selectionType, Integer min, Integer max, boolean required) {
        ModifierGroup group = CatalogTestDataFactory.modifierGroup();
        group.setSelectionType(selectionType);
        group.setMinSelections(min);
        group.setMaxSelections(max);
        group.setRequired(required);
        return group;
    }

    private void assertInvalid(ModifierGroup group) {
        assertThatThrownBy(() -> ModifierGroupValidator.validate(group))
                .isInstanceOf(CatalogValidationException.class);
    }
}
