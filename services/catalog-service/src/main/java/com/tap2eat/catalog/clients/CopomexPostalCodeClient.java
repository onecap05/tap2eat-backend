package com.tap2eat.catalog.clients;

import com.tap2eat.catalog.config.PostalCodeApiProperties;
import com.tap2eat.catalog.dtos.external.copomex.CopomexPostalCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Slf4j
@Component
public class CopomexPostalCodeClient {

    private static final String INFO_CP_PATH = "/info_cp/{postalCode}";

    private final PostalCodeApiProperties postalCodeApiProperties;
    private final RestClient restClient;

    public CopomexPostalCodeClient(PostalCodeApiProperties postalCodeApiProperties) {
        this.postalCodeApiProperties = postalCodeApiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(postalCodeApiProperties.getBaseUrl())
                .build();
    }

    public Optional<CopomexPostalCodeResponse> findByPostalCode(String postalCode) {
        try {
            CopomexPostalCodeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(INFO_CP_PATH)
                            .queryParam("type", postalCodeApiProperties.getType())
                            .queryParam("token", postalCodeApiProperties.getToken())
                            .build(postalCode)
                    )
                    .retrieve()
                    .body(CopomexPostalCodeResponse.class);

            return Optional.ofNullable(response);
        } catch (RestClientException exception) {
            log.error("Could not fetch postal code information from COPOMEX", exception);
            return Optional.empty();
        }
    }
}