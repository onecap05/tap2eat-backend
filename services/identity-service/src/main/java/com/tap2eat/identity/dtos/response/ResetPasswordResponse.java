package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after resetting the password")
public class ResetPasswordResponse {

    @Schema(description = "Operation result message", example = "Password reset successfully.")
    private String message;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}