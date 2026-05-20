package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchValidatorTest {

    @Test
    void validate_shouldAllowValidBranch() {
        assertThatCode(() -> BranchValidator.validate(CatalogTestDataFactory.branch()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectMissingRequiredFieldsAndInvalidCoordinates() {
        assertInvalid(null);
        BranchDocument missingRestaurant = CatalogTestDataFactory.branch();
        missingRestaurant.setRestaurantId(" ");
        BranchDocument missingName = CatalogTestDataFactory.branch();
        missingName.setName(null);
        BranchDocument missingAddress = CatalogTestDataFactory.branch();
        missingAddress.setFormattedAddress(" ");
        BranchDocument missingLatitude = CatalogTestDataFactory.branch();
        missingLatitude.setLatitude(null);
        BranchDocument invalidCoordinates = CatalogTestDataFactory.branch();
        invalidCoordinates.setLatitude(91.0);

        assertInvalid(missingRestaurant);
        assertInvalid(missingName);
        assertInvalid(missingAddress);
        assertInvalid(missingLatitude);
        assertInvalid(invalidCoordinates);
    }

    @Test
    void validate_shouldRejectInvalidPhoneAndMissingRequiredSchedule() {
        BranchDocument invalidPhone = CatalogTestDataFactory.branch();
        invalidPhone.setPhoneNumber("abc");
        BranchDocument noAvailability = CatalogTestDataFactory.branch();
        noAvailability.setAvailability(null);
        BranchDocument emptySchedule = CatalogTestDataFactory.branch();
        emptySchedule.getAvailability().setWeeklySchedule(List.of());
        BranchDocument noEnabledDays = CatalogTestDataFactory.branch();
        noEnabledDays.getAvailability().getWeeklySchedule().getFirst().setEnabled(Boolean.FALSE);
        noEnabledDays.getAvailability().getWeeklySchedule().getFirst().setTimeRanges(List.of());

        assertInvalid(invalidPhone);
        assertInvalid(noAvailability);
        assertInvalid(emptySchedule);
        assertInvalid(noEnabledDays);
    }

    private void assertInvalid(BranchDocument branch) {
        assertThatThrownBy(() -> BranchValidator.validate(branch))
                .isInstanceOf(CatalogValidationException.class);
    }
}
