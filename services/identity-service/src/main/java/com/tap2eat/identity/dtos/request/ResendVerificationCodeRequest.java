package com.tap2eat.identity.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResendVerificationCodeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is incorrect")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    public ResendVerificationCodeRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}