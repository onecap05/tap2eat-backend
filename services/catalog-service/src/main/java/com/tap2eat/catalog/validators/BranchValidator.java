package com.tap2eat.catalog.validators;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;

import java.util.List;

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
        validateRequiredSchedule(branch.getAvailability());
    }

    private static void validateRequiredSchedule(AvailabilityConfig availability) {
        if (availability == null
                || availability.getWeeklySchedule() == null
                || availability.getWeeklySchedule().isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        List<DailyAvailability> enabledDays = availability.getWeeklySchedule().stream()
                .filter(day -> Boolean.TRUE.equals(day.getEnabled()))
                .toList();

        if (enabledDays.isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
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