package com.tap2eat.catalog.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCatalogException_shouldReturnStructuredErrorResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponse> response = handler.handleCatalogException(
                new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA),
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CATALOG_006");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleGenericException_shouldReturnInternalErrorResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CATALOG_010");
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }
}
