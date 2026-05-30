package com.tap2eat.identity.dtos;

import com.tap2eat.identity.dtos.request.ForgotPasswordRequest;
import com.tap2eat.identity.dtos.request.LoginRequest;
import com.tap2eat.identity.dtos.request.RefreshTokenRequest;
import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.request.ResetPasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void registerRequest_whenEmailIsInvalid_shouldFailValidation() {
        RegisterRequest request = validRegisterRequest();
        request.setEmail("invalid-email");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "email", "Email format is incorrect"));
    }

    @Test
    void registerRequest_whenRequiredFieldsAreBlank_shouldFailValidation() {
        RegisterRequest request = validRegisterRequest();
        request.setEmail(" ");
        request.setPassword(" ");
        request.setRole(" ");
        request.setFirstName(" ");
        request.setLastName(" ");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "email", "Email is required"));
        assertTrue(hasViolation(violations, "password", "Password is required"));
        assertTrue(hasViolation(violations, "role", "Role is required"));
        assertTrue(hasViolation(violations, "firstName", "First name is required"));
        assertTrue(hasViolation(violations, "lastName", "Last name is required"));
    }

    @Test
    void registerRequest_whenPhoneIsTooLong_shouldFailValidation() {
        RegisterRequest request = validRegisterRequest();
        request.setPhone("1".repeat(31));

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "phone", "Phone must not exceed 30 characters"));
    }

    @Test
    void loginRequest_whenPasswordIsTooShort_shouldFailValidation() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("short");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "password", "Password must be between 8 and 72 characters"));
    }

    @Test
    void refreshTokenRequest_whenTokenIsBlank_shouldFailValidation() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(" ");

        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "refreshToken", "Refresh token is required"));
    }

    @Test
    void forgotPasswordRequest_whenEmailIsInvalid_shouldFailValidation() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("invalid-email");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "email", "Email format is incorrect"));
    }

    @Test
    void resetPasswordRequest_whenPasswordIsTooShort_shouldFailValidation() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setCode("123456");
        request.setNewPassword("short");

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(hasViolation(violations, "newPassword", "Password must be between 8 and 72 characters"));
    }

    @Test
    void registerRequest_whenAllFieldsAreValid_shouldPassValidation() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validRegisterRequest());

        assertTrue(violations.isEmpty());
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("Strong123!");
        request.setRole("CUSTOMER");
        request.setFirstName("Angel");
        request.setLastName("Ruiz");
        request.setPhone("2281234567");
        return request;
    }

    private <T> boolean hasViolation(Set<ConstraintViolation<T>> violations, String field, String message) {
        return violations.stream().anyMatch(violation ->
                field.equals(violation.getPropertyPath().toString())
                        && message.equals(violation.getMessage())
        );
    }
}
