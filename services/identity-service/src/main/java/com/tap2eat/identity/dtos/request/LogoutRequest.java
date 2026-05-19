package com.tap2eat.identity.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to logout and revoke a refresh token")
public class LogoutRequest {

    @Schema(
            description = "Refresh token to revoke",
            example = "b6d7f7b3-9a80-4d95-9cbe-5a7c5b2b2f21",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token is required")
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