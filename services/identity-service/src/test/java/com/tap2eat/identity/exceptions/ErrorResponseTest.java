package com.tap2eat.identity.exceptions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {

    @Test
    void constructorsAndSetters_shouldExposeValues() {
        ErrorResponse emptyResponse = new ErrorResponse();
        emptyResponse.setMessage("validation failed");
        emptyResponse.setErrors(Map.of("email", "Email is required"));

        assertEquals("validation failed", emptyResponse.getMessage());
        assertEquals("Email is required", emptyResponse.getErrors().get("email"));

        ErrorResponse messageResponse = new ErrorResponse("conflict");
        assertEquals("conflict", messageResponse.getMessage());

        ErrorResponse fieldErrorResponse = new ErrorResponse(
                "validation failed",
                Map.of("password", "Password is required")
        );
        assertEquals("validation failed", fieldErrorResponse.getMessage());
        assertEquals("Password is required", fieldErrorResponse.getErrors().get("password"));
    }
}
