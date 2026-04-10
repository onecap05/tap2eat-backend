package com.tap2eat.identity.dtos.request;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {

    @NotBlank(message = "Refresh token is required.")
    private String refreshToken;

    public LogoutRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}