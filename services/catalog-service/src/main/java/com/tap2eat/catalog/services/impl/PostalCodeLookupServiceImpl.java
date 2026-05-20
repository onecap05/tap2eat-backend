package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.clients.CopomexPostalCodeClient;
import com.tap2eat.catalog.dtos.external.copomex.CopomexPostalCodeResponse;
import com.tap2eat.catalog.dtos.external.copomex.CopomexPostalCodeResponse.CopomexPostalCodeData;
import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.PostalCodeDocument;
import com.tap2eat.catalog.repositories.IPostalCodeRepository;
import com.tap2eat.catalog.services.IPostalCodeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostalCodeLookupServiceImpl implements IPostalCodeLookupService {

    private static final String MEXICO_POSTAL_CODE_PATTERN = "^\\d{5}$";
    private static final String DEFAULT_COUNTRY = "México";

    private final IPostalCodeRepository postalCodeRepository;
    private final CopomexPostalCodeClient copomexPostalCodeClient;

    @Override
    public PostalCodeLookupResponse lookupByPostalCode(String postalCode) {
        String normalizedPostalCode = normalizeRequiredPostalCode(postalCode);

        List<PostalCodeDocument> cachedPostalCodes =
                postalCodeRepository.findAllByPostalCode(normalizedPostalCode);

        if (!cachedPostalCodes.isEmpty()) {
            return buildResponse(normalizedPostalCode, cachedPostalCodes);
        }

        List<PostalCodeDocument> fetchedPostalCodes = fetchAndCachePostalCode(normalizedPostalCode);

        return buildResponse(normalizedPostalCode, fetchedPostalCodes);
    }

    private List<PostalCodeDocument> fetchAndCachePostalCode(String postalCode) {
        CopomexPostalCodeResponse externalResponse = copomexPostalCodeClient.findByPostalCode(postalCode)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));

        if (externalResponse.hasError() || externalResponse.response() == null) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }

        CopomexPostalCodeData response = externalResponse.response();

        if (isBlank(response.cp()) || response.asentamiento() == null || response.asentamiento().isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }

        List<PostalCodeDocument> postalCodeDocuments = response.asentamiento().stream()
                .filter(neighborhood -> !isBlank(neighborhood))
                .distinct()
                .map(neighborhood -> toPostalCodeDocument(response, neighborhood))
                .toList();

        if (postalCodeDocuments.isEmpty()) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }

        return postalCodeRepository.saveAll(postalCodeDocuments);
    }

    private PostalCodeDocument toPostalCodeDocument(
            CopomexPostalCodeData response,
            String neighborhood
    ) {
        PostalCodeDocument document = new PostalCodeDocument();
        document.setPostalCode(normalize(response.cp()));
        document.setNeighborhood(normalize(neighborhood));
        document.setMunicipality(normalize(response.municipio()));
        document.setState(normalize(response.estado()));
        document.setCity(normalize(resolveCity(response)));
        document.setCountry(normalize(resolveCountry(response)));

        return document;
    }

    private PostalCodeLookupResponse buildResponse(
            String postalCode,
            List<PostalCodeDocument> postalCodeDocuments
    ) {
        PostalCodeDocument firstMatch = postalCodeDocuments.get(0);

        List<String> neighborhoods = postalCodeDocuments.stream()
                .map(PostalCodeDocument::getNeighborhood)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        return new PostalCodeLookupResponse(
                postalCode,
                firstMatch.getCity(),
                firstMatch.getMunicipality(),
                firstMatch.getState(),
                firstMatch.getCountry(),
                neighborhoods
        );
    }

    private String normalizeRequiredPostalCode(String postalCode) {
        String normalizedPostalCode = normalize(postalCode);

        if (isBlank(normalizedPostalCode) || !normalizedPostalCode.matches(MEXICO_POSTAL_CODE_PATTERN)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        return normalizedPostalCode;
    }

    private String resolveCity(CopomexPostalCodeData response) {
        if (!isBlank(response.ciudad())) {
            return response.ciudad();
        }

        return response.municipio();
    }

    private String resolveCountry(CopomexPostalCodeData response) {
        if (!isBlank(response.pais())) {
            return response.pais();
        }

        return DEFAULT_COUNTRY;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}