package com.tap2eat.identity.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        handler = new GlobalExceptionHandler(messageSource);
    }

    @Test
    void handleEmailAlreadyRegistered_shouldReturnConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleEmailAlreadyRegistered(new EmailAlreadyRegisteredException("duplicate email"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("duplicate email", response.getBody().getMessage());
    }

    @Test
    void handleInvalidRole_shouldReturnBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidRole(new InvalidRoleException("invalid role"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid role", response.getBody().getMessage());
    }

    @Test
    void handleWeakPassword_shouldReturnBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleWeakPassword(new WeakPasswordException("weak password"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("weak password", response.getBody().getMessage());
    }

    @Test
    void handleInvalidCredentials_shouldReturnUnauthorized() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("invalid credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("invalid credentials", response.getBody().getMessage());
    }

    @Test
    void handleForbiddenAccountStates_shouldReturnForbidden() {
        ResponseEntity<ErrorResponse> inactiveResponse =
                handler.handleInactiveAccount(new InactiveAccountException("inactive"));
        ResponseEntity<ErrorResponse> unverifiedResponse =
                handler.handleEmailNotVerified(new EmailNotVerifiedException("unverified"));

        assertEquals(HttpStatus.FORBIDDEN, inactiveResponse.getStatusCode());
        assertEquals("inactive", inactiveResponse.getBody().getMessage());
        assertEquals(HttpStatus.FORBIDDEN, unverifiedResponse.getStatusCode());
        assertEquals("unverified", unverifiedResponse.getBody().getMessage());
    }

    @Test
    void handleTokenAndCodeExceptions_shouldReturnExpectedStatuses() {
        ResponseEntity<ErrorResponse> refreshTokenResponse =
                handler.handleInvalidRefreshToken(new InvalidRefreshTokenException("invalid refresh"));
        ResponseEntity<ErrorResponse> emailCodeResponse =
                handler.handleInvalidEmailVerificationCode(new InvalidEmailVerificationCodeException("invalid email code"));
        ResponseEntity<ErrorResponse> resetCodeResponse =
                handler.handleInvalidPasswordResetCode(new InvalidPasswordResetCodeException("invalid reset code"));

        assertEquals(HttpStatus.UNAUTHORIZED, refreshTokenResponse.getStatusCode());
        assertEquals("invalid refresh", refreshTokenResponse.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, emailCodeResponse.getStatusCode());
        assertEquals("invalid email code", emailCodeResponse.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, resetCodeResponse.getStatusCode());
        assertEquals("invalid reset code", resetCodeResponse.getBody().getMessage());
    }

    @Test
    void handleGeneralException_shouldReturnInternalServerErrorWithGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("auth.unexpected.error", response.getBody().getMessage());
    }
}
