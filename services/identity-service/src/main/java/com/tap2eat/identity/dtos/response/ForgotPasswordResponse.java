package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after requesting a password reset code")
public class ForgotPasswordResponse {

    @Schema(description = "Operation result message", example = "If the email exists, a recovery code has been sent.")
    private String message;

    public ForgotPasswordResponse() {
    }

    public ForgotPasswordResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}