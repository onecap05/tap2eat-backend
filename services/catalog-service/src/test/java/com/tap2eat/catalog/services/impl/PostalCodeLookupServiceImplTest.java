package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.clients.CopomexPostalCodeClient;
import com.tap2eat.catalog.dtos.external.copomex.CopomexPostalCodeResponse;
import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.PostalCodeDocument;
import com.tap2eat.catalog.repositories.IPostalCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostalCodeLookupServiceImplTest {

    private IPostalCodeRepository postalCodeRepository;
    private CopomexPostalCodeClient copomexPostalCodeClient;
    private PostalCodeLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        postalCodeRepository = mock(IPostalCodeRepository.class);
        copomexPostalCodeClient = mock(CopomexPostalCodeClient.class);
        service = new PostalCodeLookupServiceImpl(postalCodeRepository, copomexPostalCodeClient);
    }

    @Test
    void lookupByPostalCode_shouldReturnCachedPostalCodeSortedNeighborhoods() {
        when(postalCodeRepository.findAllByPostalCode("06000")).thenReturn(List.of(
                postalCode("06000", "Zocalo", "Ciudad de Mexico", "Cuauhtemoc", "CDMX", "Mexico"),
                postalCode("06000", "Centro", "Ciudad de Mexico", "Cuauhtemoc", "CDMX", "Mexico"),
                postalCode("06000", "Centro", "Ciudad de Mexico", "Cuauhtemoc", "CDMX", "Mexico")
        ));

        PostalCodeLookupResponse response = service.lookupByPostalCode(" 06000 ");

        assertThat(response.postalCode()).isEqualTo("06000");
        assertThat(response.neighborhoods()).containsExactly("Centro", "Zocalo");
    }

    @Test
    void lookupByPostalCode_shouldFetchCacheAndNormalizeExternalResponse() {
        when(postalCodeRepository.findAllByPostalCode("06000")).thenReturn(List.of());
        when(copomexPostalCodeClient.findByPostalCode("06000")).thenReturn(Optional.of(copomexResponse(
                false,
                " 06000 ",
                List.of(" Centro ", "", "Roma"),
                "Cuauhtemoc",
                "CDMX",
                "",
                ""
        )));
        when(postalCodeRepository.saveAll(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        PostalCodeLookupResponse response = service.lookupByPostalCode("06000");

        assertThat(response.city()).isEqualTo("Cuauhtemoc");
        assertThat(response.country()).contains("M");
        assertThat(response.neighborhoods()).containsExactly("Centro", "Roma");
    }

    @Test
    void lookupByPostalCode_shouldRejectInvalidPostalCodesAndMissingExternalData() {
        assertThatThrownBy(() -> service.lookupByPostalCode("abc")).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> service.lookupByPostalCode(null)).isInstanceOf(CatalogValidationException.class);

        when(postalCodeRepository.findAllByPostalCode("06000")).thenReturn(List.of());
        when(copomexPostalCodeClient.findByPostalCode("06000")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lookupByPostalCode("06000")).isInstanceOf(CatalogValidationException.class);

        when(copomexPostalCodeClient.findByPostalCode("06000")).thenReturn(Optional.of(copomexResponse(true, "06000", List.of("Centro"), "Cuauhtemoc", "CDMX", null, "Mexico")));
        assertThatThrownBy(() -> service.lookupByPostalCode("06000")).isInstanceOf(CatalogValidationException.class);

        when(copomexPostalCodeClient.findByPostalCode("06000")).thenReturn(Optional.of(copomexResponse(false, " ", List.of("Centro"), "Cuauhtemoc", "CDMX", null, "Mexico")));
        assertThatThrownBy(() -> service.lookupByPostalCode("06000")).isInstanceOf(CatalogValidationException.class);

        when(copomexPostalCodeClient.findByPostalCode("06000")).thenReturn(Optional.of(copomexResponse(false, "06000", List.of("", " "), "Cuauhtemoc", "CDMX", null, "Mexico")));
        assertThatThrownBy(() -> service.lookupByPostalCode("06000")).isInstanceOf(CatalogValidationException.class);
    }

    private PostalCodeDocument postalCode(
            String code,
            String neighborhood,
            String city,
            String municipality,
            String state,
            String country
    ) {
        PostalCodeDocument document = new PostalCodeDocument();
        document.setPostalCode(code);
        document.setNeighborhood(neighborhood);
        document.setCity(city);
        document.setMunicipality(municipality);
        document.setState(state);
        document.setCountry(country);
        return document;
    }

    private CopomexPostalCodeResponse copomexResponse(
            boolean error,
            String cp,
            List<String> neighborhoods,
            String municipality,
            String state,
            String city,
            String country
    ) {
        return new CopomexPostalCodeResponse(
                error,
                error ? 1 : 0,
                null,
                new CopomexPostalCodeResponse.CopomexPostalCodeData(
                        cp,
                        neighborhoods,
                        "Colonia",
                        municipality,
                        state,
                        city,
                        country
                )
        );
    }
}
