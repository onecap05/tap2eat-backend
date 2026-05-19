package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after successful authentication")
public class LoginResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh token used to renew the access token", example = "b6d7f7b3-9a80-4d95-9cbe-5a7c5b2b2f21")
    private String refreshToken;

    @Schema(description = "Authentication token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token expiration time in milliseconds", example = "120000")
    private long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}