package com.tap2eat.identity.controllers;

import com.tap2eat.identity.dtos.request.ForgotPasswordRequest;
import com.tap2eat.identity.dtos.request.LoginRequest;
import com.tap2eat.identity.dtos.request.LogoutRequest;
import com.tap2eat.identity.dtos.request.RefreshTokenRequest;
import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.request.ResendVerificationCodeRequest;
import com.tap2eat.identity.dtos.request.ResetPasswordRequest;
import com.tap2eat.identity.dtos.request.VerifyEmailRequest;
import com.tap2eat.identity.dtos.response.ForgotPasswordResponse;
import com.tap2eat.identity.dtos.response.LoginResponse;
import com.tap2eat.identity.dtos.response.MeResponse;
import com.tap2eat.identity.dtos.response.RegisterResponse;
import com.tap2eat.identity.dtos.response.ResendVerificationCodeResponse;
import com.tap2eat.identity.dtos.response.ResetPasswordResponse;
import com.tap2eat.identity.dtos.response.TokenRefreshResponse;
import com.tap2eat.identity.dtos.response.VerifyEmailResponse;
import com.tap2eat.identity.services.IAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IAuthService authService;

    @Test
    void register_shouldReturnCreatedResponse() {
        AuthController controller = new AuthController(authService);
        RegisterRequest request = new RegisterRequest();
        RegisterResponse serviceResponse =
                new RegisterResponse(UUID.randomUUID(), "user@example.com", "CUSTOMER", "created");
        when(authService.registerAccount(request)).thenReturn(serviceResponse);

        ResponseEntity<RegisterResponse> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
    }

    @Test
    void login_shouldReturnOkResponse() {
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest();
        LoginResponse serviceResponse = new LoginResponse("access", "refresh", "Bearer", 120000L);
        when(authService.login(request)).thenReturn(serviceResponse);

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
    }

    @Test
    void me_shouldUseAuthenticationName() {
        AuthController controller = new AuthController(authService);
        Authentication authentication = mock(Authentication.class);
        MeResponse serviceResponse = new MeResponse(
                UUID.randomUUID(),
                "user@example.com",
                "CUSTOMER",
                true,
                true,
                "Angel",
                "Ruiz",
                null
        );

        when(authentication.getName()).thenReturn("user@example.com");
        when(authService.getCurrentAccount("user@example.com")).thenReturn(serviceResponse);

        ResponseEntity<MeResponse> response = controller.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
    }

    @Test
    void logout_shouldReturnNoContent() {
        AuthController controller = new AuthController(authService);
        LogoutRequest request = new LogoutRequest();

        ResponseEntity<Void> response = controller.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).logout(request);
    }

    @Test
    void refresh_shouldReturnOkResponse() {
        AuthController controller = new AuthController(authService);
        RefreshTokenRequest request = new RefreshTokenRequest();
        TokenRefreshResponse serviceResponse = new TokenRefreshResponse("access", "refresh", "Bearer");
        when(authService.refreshToken(request)).thenReturn(serviceResponse);

        ResponseEntity<TokenRefreshResponse> response = controller.refresh(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
    }

    @Test
    void emailAndPasswordFlows_shouldReturnOkResponses() {
        AuthController controller = new AuthController(authService);
        VerifyEmailRequest verifyEmailRequest = new VerifyEmailRequest();
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        ResendVerificationCodeRequest resendRequest = new ResendVerificationCodeRequest();

        when(authService.verifyEmail(verifyEmailRequest)).thenReturn(new VerifyEmailResponse("verified"));
        when(authService.forgotPassword(forgotPasswordRequest)).thenReturn(new ForgotPasswordResponse("sent"));
        when(authService.resetPassword(resetPasswordRequest)).thenReturn(new ResetPasswordResponse("reset"));
        when(authService.resendVerificationCode(resendRequest))
                .thenReturn(new ResendVerificationCodeResponse("resent"));

        assertEquals(HttpStatus.OK, controller.verifyEmail(verifyEmailRequest).getStatusCode());
        assertEquals(HttpStatus.OK, controller.forgotPassword(forgotPasswordRequest).getStatusCode());
        assertEquals(HttpStatus.OK, controller.resetPassword(resetPasswordRequest).getStatusCode());
        assertEquals(HttpStatus.OK, controller.resendVerificationCode(resendRequest).getStatusCode());
    }
}
