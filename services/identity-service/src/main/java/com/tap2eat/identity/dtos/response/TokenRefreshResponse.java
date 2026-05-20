package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after refreshing an access token")
public class TokenRefreshResponse {

    @Schema(description = "New JWT access token", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh token associated with the session", example = "b6d7f7b3-9a80-4d95-9cbe-5a7c5b2b2f21")
    private String refreshToken;

    @Schema(description = "Authentication token type", example = "Bearer")
    private String tokenType = "Bearer";

    public TokenRefreshResponse() {
    }

    public TokenRefreshResponse(String accessToken, String refreshToken, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
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
}