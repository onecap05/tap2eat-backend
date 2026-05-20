package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after email verification")
public class VerifyEmailResponse {

    @Schema(description = "Operation result message", example = "Email verified successfully.")
    private String message;

    public VerifyEmailResponse() {
    }

    public VerifyEmailResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}