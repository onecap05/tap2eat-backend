package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.BranchDocument;

public final class BranchValidator {

    private BranchValidator() {
    }

    public static void validate(BranchDocument branch) {
        if (branch == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        validateRequiredFields(branch);
        validateCoordinates(branch);
        validatePhoneNumber(branch.getPhoneNumber());

        AvailabilityValidator.validate(branch.getAvailability());
    }

    private static void validateRequiredFields(BranchDocument branch) {
        if (isBlank(branch.getRestaurantId())
                || isBlank(branch.getName())
                || isBlank(branch.getFormattedAddress())
                || branch.getLatitude() == null
                || branch.getLongitude() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private static void validateCoordinates(BranchDocument branch) {
        Double latitude = branch.getLatitude();
        Double longitude = branch.getLongitude();

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        String normalized = phoneNumber.replaceAll("[\\s\\-()]+", "");
        if (!normalized.matches("^\\+?[0-9]{8,15}$")) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}