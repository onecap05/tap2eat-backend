package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.services.IPostalCodeLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressValidationServiceImplTest {

    private IPostalCodeLookupService postalCodeLookupService;
    private AddressValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        postalCodeLookupService = mock(IPostalCodeLookupService.class);
        service = new AddressValidationServiceImpl(postalCodeLookupService);
    }

    @Test
    void validateMexicanAddress_shouldAllowMatchingAddressWithAccentNormalization() {
        when(postalCodeLookupService.lookupByPostalCode("06000")).thenReturn(lookup());

        assertThatCode(() -> service.validateMexicanAddress(
                "06000",
                " centro ",
                "ciudad de mexico",
                "cdmx",
                "México"
        )).doesNotThrowAnyException();
    }

    @Test
    void validateMexicanAddress_shouldAllowMunicipalityWhenCityDoesNotMatchCityField() {
        when(postalCodeLookupService.lookupByPostalCode("06000")).thenReturn(lookup());

        assertThatCode(() -> service.validateMexicanAddress(
                "06000",
                "Centro",
                "Cuauhtemoc",
                "CDMX",
                "Mexico"
        )).doesNotThrowAnyException();
    }

    @Test
    void validateMexicanAddress_shouldRejectMissingFieldsCountryAndPostalCodeFormat() {
        assertInvalid(" ", "Centro", "Ciudad de Mexico", "CDMX", "Mexico");
        assertInvalid("06000", " ", "Ciudad de Mexico", "CDMX", "Mexico");
        assertInvalid("06000", "Centro", " ", "CDMX", "Mexico");
        assertInvalid("06000", "Centro", "Ciudad de Mexico", " ", "Mexico");
        assertInvalid("06000", "Centro", "Ciudad de Mexico", "CDMX", "USA");
        assertInvalid("ABC", "Centro", "Ciudad de Mexico", "CDMX", "Mexico");
    }

    @Test
    void validateMexicanAddress_shouldRejectStateCityOrNeighborhoodMismatch() {
        when(postalCodeLookupService.lookupByPostalCode("06000")).thenReturn(lookup());

        assertInvalid("06000", "Centro", "Ciudad de Mexico", "Jalisco", "Mexico");
        assertInvalid("06000", "Centro", "Guadalajara", "CDMX", "Mexico");
        assertInvalid("06000", "Roma", "Ciudad de Mexico", "CDMX", "Mexico");
    }

    @Test
    void validateMexicanAddress_shouldRejectEmptyNeighborhoodCatalog() {
        when(postalCodeLookupService.lookupByPostalCode("06000")).thenReturn(new PostalCodeLookupResponse(
                "06000",
                "Ciudad de Mexico",
                "Cuauhtemoc",
                "CDMX",
                "Mexico",
                List.of()
        ));

        assertInvalid("06000", "Centro", "Ciudad de Mexico", "CDMX", "Mexico");
    }

    private void assertInvalid(String postalCode, String neighborhood, String city, String state, String country) {
        assertThatThrownBy(() -> service.validateMexicanAddress(postalCode, neighborhood, city, state, country))
                .isInstanceOf(CatalogValidationException.class);
    }

    private PostalCodeLookupResponse lookup() {
        return new PostalCodeLookupResponse(
                "06000",
                "Ciudad de Mexico",
                "Cuauhtemoc",
                "CDMX",
                "Mexico",
                List.of("Centro")
        );
    }
}
