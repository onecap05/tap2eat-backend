package com.tap2eat.identity.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to reset an account password")
public class ResetPasswordRequest {

    @Schema(
            description = "Email address of the account",
            example = "cliente1@ejemplo.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is incorrect")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(
            description = "Password reset code sent to the user's email",
            example = "654321",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Reset code is required")
    private String code;

    @Schema(
            description = "New password for the account",
            example = "Nueva123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "New password is required")
    @Size(min =8, max = 72, message = "Password must be between 8 and 72 characters")
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}