package com.tap2eat.catalog.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogExceptionTest {

    @Test
    void exceptions_shouldExposeErrorCodeAndCustomMessages() {
        CatalogException catalogException = new CatalogException(CatalogErrorCode.INTERNAL_ERROR, "custom");
        CatalogValidationException validationException = new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA, "bad branch");
        ResourceNotFoundException notFound = new ResourceNotFoundException("missing");
        UnauthorizedCatalogAccessException unauthorized = new UnauthorizedCatalogAccessException("forbidden");

        assertThat(catalogException.getErrorCode()).isEqualTo(CatalogErrorCode.INTERNAL_ERROR);
        assertThat(catalogException.getMessage()).isEqualTo("custom");
        assertThat(validationException.getMessage()).isEqualTo("bad branch");
        assertThat(notFound.getErrorCode()).isEqualTo(CatalogErrorCode.RESOURCE_NOT_FOUND);
        assertThat(notFound.getMessage()).isEqualTo("missing");
        assertThat(new ResourceNotFoundException().getErrorCode()).isEqualTo(CatalogErrorCode.RESOURCE_NOT_FOUND);
        assertThat(unauthorized.getErrorCode()).isEqualTo(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        assertThat(unauthorized.getMessage()).isEqualTo("forbidden");
        assertThat(new UnauthorizedCatalogAccessException().getErrorCode())
                .isEqualTo(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
    }
}
