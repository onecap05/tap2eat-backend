package com.tap2eat.identity.dtos;

import com.tap2eat.identity.dtos.response.ForgotPasswordResponse;
import com.tap2eat.identity.dtos.response.LoginResponse;
import com.tap2eat.identity.dtos.response.MeResponse;
import com.tap2eat.identity.dtos.response.RegisterResponse;
import com.tap2eat.identity.dtos.response.ResendVerificationCodeResponse;
import com.tap2eat.identity.dtos.response.ResetPasswordResponse;
import com.tap2eat.identity.dtos.response.TokenRefreshResponse;
import com.tap2eat.identity.dtos.response.VerifyEmailResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseDtoTest {

    @Test
    void loginResponse_shouldExposeConstructorAndSetterValues() {
        LoginResponse response = new LoginResponse("access", "refresh", "Bearer", 120000L);

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(120000L, response.getExpiresIn());

        response.setAccessToken("new-access");
        response.setRefreshToken("new-refresh");
        response.setTokenType("Token");
        response.setExpiresIn(240000L);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("Token", response.getTokenType());
        assertEquals(240000L, response.getExpiresIn());
    }

    @Test
    void registerResponse_shouldExposeConstructorAndSetterValues() {
        UUID id = UUID.randomUUID();
        RegisterResponse response = new RegisterResponse(id, "user@example.com", "CUSTOMER", "created");

        assertEquals(id, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("created", response.getMessage());

        UUID newId = UUID.randomUUID();
        response.setId(newId);
        response.setEmail("new@example.com");
        response.setRole("RESTAURANT_OWNER");
        response.setMessage("updated");

        assertEquals(newId, response.getId());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("RESTAURANT_OWNER", response.getRole());
        assertEquals("updated", response.getMessage());
    }

    @Test
    void meResponse_shouldExposeConstructorAndSetterValues() {
        UUID id = UUID.randomUUID();
        MeResponse response = new MeResponse(
                id,
                "user@example.com",
                "CUSTOMER",
                true,
                true,
                "Angel",
                "Ruiz",
                "2281234567"
        );

        assertEquals(id, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertTrue(response.getIsActive());
        assertTrue(response.getEmailVerified());
        assertEquals("Angel", response.getFirstName());
        assertEquals("Ruiz", response.getLastName());
        assertEquals("2281234567", response.getPhone());

        UUID newId = UUID.randomUUID();
        response.setId(newId);
        response.setEmail("new@example.com");
        response.setRole("RESTAURANT_OWNER");
        response.setIsActive(false);
        response.setEmailVerified(false);
        response.setFirstName("Ana");
        response.setLastName("Lopez");
        response.setPhone("555");

        assertEquals(newId, response.getId());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("RESTAURANT_OWNER", response.getRole());
        assertEquals(false, response.getIsActive());
        assertEquals(false, response.getEmailVerified());
        assertEquals("Ana", response.getFirstName());
        assertEquals("Lopez", response.getLastName());
        assertEquals("555", response.getPhone());
    }

    @Test
    void tokenRefreshResponse_shouldExposeConstructorAndSetterValues() {
        TokenRefreshResponse response = new TokenRefreshResponse("access", "refresh", "Bearer");

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        response.setAccessToken("new-access");
        response.setRefreshToken("new-refresh");
        response.setTokenType("Token");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("Token", response.getTokenType());
    }

    @Test
    void singleMessageResponses_shouldExposeConstructorAndSetterValues() {
        ForgotPasswordResponse forgotPasswordResponse = new ForgotPasswordResponse("forgot");
        ResetPasswordResponse resetPasswordResponse = new ResetPasswordResponse("reset");
        VerifyEmailResponse verifyEmailResponse = new VerifyEmailResponse("verified");
        ResendVerificationCodeResponse resendResponse = new ResendVerificationCodeResponse("resent");

        assertEquals("forgot", forgotPasswordResponse.getMessage());
        assertEquals("reset", resetPasswordResponse.getMessage());
        assertEquals("verified", verifyEmailResponse.getMessage());
        assertEquals("resent", resendResponse.getMessage());

        forgotPasswordResponse.setMessage("forgot-updated");
        resetPasswordResponse.setMessage("reset-updated");
        verifyEmailResponse.setMessage("verified-updated");
        resendResponse.setMessage("resent-updated");

        assertEquals("forgot-updated", forgotPasswordResponse.getMessage());
        assertEquals("reset-updated", resetPasswordResponse.getMessage());
        assertEquals("verified-updated", verifyEmailResponse.getMessage());
        assertEquals("resent-updated", resendResponse.getMessage());
    }
}
