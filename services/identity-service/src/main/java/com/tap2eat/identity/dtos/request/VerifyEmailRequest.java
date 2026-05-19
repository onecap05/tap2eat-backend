package com.tap2eat.identity.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to verify an account email")
public class VerifyEmailRequest {

    @Schema(
            description = "Verification code sent to the user's email",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Verification code is required")
    private String code;

    public VerifyEmailRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}