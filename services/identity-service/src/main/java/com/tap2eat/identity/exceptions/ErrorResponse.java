package com.tap2eat.identity.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Standard API error response")
public class ErrorResponse {

    @Schema(description = "General error message", example = "Validation failed.")
    private String message;

    @Schema(
            description = "Field validation errors when applicable",
            example = "{\"email\":\"Email is required\",\"password\":\"Password is required\"}"
    )
    private Map<String, String> errors;

    public ErrorResponse() {
    }

    public ErrorResponse(String message) {
        this.message = message;
    }

    public ErrorResponse(String message, Map<String, String> errors) {
        this.message = message;
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}