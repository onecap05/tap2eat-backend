package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after requesting a new verification code")
public class ResendVerificationCodeResponse {

    @Schema(description = "Operation result message", example = "If the account exists, a new verification code has been sent.")
    private String message;

    public ResendVerificationCodeResponse() {
    }

    public ResendVerificationCodeResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}