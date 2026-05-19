package com.tap2eat.identity.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to generate a password reset code")
public class ForgotPasswordRequest {

    @Schema(
            description = "Email address of the account",
            example = "cliente1@ejemplo.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is incorrect")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    public ForgotPasswordRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}