package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.services.IAddressValidationService;
import com.tap2eat.catalog.services.IPostalCodeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressValidationServiceImpl implements IAddressValidationService {

    private static final String MEXICO = "mexico";
    private static final String MEXICO_WITH_ACCENT = "méxico";
    private static final String MEXICO_POSTAL_CODE_PATTERN = "^\\d{5}$";

    private final IPostalCodeLookupService postalCodeLookupService;

    @Override
    public void validateMexicanAddress(
            String postalCode,
            String neighborhood,
            String city,
            String state,
            String country
    ) {
        validateRequiredAddressFields(postalCode, neighborhood, city, state, country);

        if (!isMexico(country)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        if (!postalCode.matches(MEXICO_POSTAL_CODE_PATTERN)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        PostalCodeLookupResponse lookup = postalCodeLookupService.lookupByPostalCode(postalCode);

        validateStateMatches(state, lookup.state());
        validateCityOrMunicipalityMatches(city, lookup.city(), lookup.municipality());
        validateNeighborhoodMatches(neighborhood, lookup.neighborhoods());
    }

    private void validateRequiredAddressFields(
            String postalCode,
            String neighborhood,
            String city,
            String state,
            String country
    ) {
        if (isBlank(postalCode)
                || isBlank(neighborhood)
                || isBlank(city)
                || isBlank(state)
                || isBlank(country)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private void validateStateMatches(String state, String expectedState) {
        if (!equalsNormalized(state, expectedState)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private void validateCityOrMunicipalityMatches(
            String city,
            String expectedCity,
            String expectedMunicipality
    ) {
        boolean matchesCity = equalsNormalized(city, expectedCity);
        boolean matchesMunicipality = equalsNormalized(city, expectedMunicipality);

        if (!matchesCity && !matchesMunicipality) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private void validateNeighborhoodMatches(String neighborhood, List<String> validNeighborhoods) {
        if (validNeighborhoods == null || validNeighborhoods.isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        boolean exists = validNeighborhoods.stream()
                .anyMatch(validNeighborhood -> equalsNormalized(neighborhood, validNeighborhood));

        if (!exists) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private boolean isMexico(String country) {
        String normalizedCountry = normalize(country);

        return MEXICO.equals(normalizedCountry) || MEXICO_WITH_ACCENT.equals(normalizedCountry);
    }

    private boolean equalsNormalized(String firstValue, String secondValue) {
        if (isBlank(firstValue) || isBlank(secondValue)) {
            return false;
        }

        return normalize(firstValue).equals(normalize(secondValue));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}