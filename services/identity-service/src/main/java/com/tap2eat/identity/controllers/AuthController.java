package com.tap2eat.identity.controllers;

import com.tap2eat.identity.dtos.request.LogoutRequest;
import com.tap2eat.identity.dtos.request.RefreshTokenRequest;
import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.response.RegisterResponse;
import com.tap2eat.identity.dtos.response.TokenRefreshResponse;
import com.tap2eat.identity.services.IAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tap2eat.identity.dtos.response.MeResponse;
import org.springframework.security.core.Authentication;
import com.tap2eat.identity.dtos.request.LoginRequest;
import com.tap2eat.identity.dtos.response.LoginResponse;
import com.tap2eat.identity.dtos.request.VerifyEmailRequest;
import com.tap2eat.identity.dtos.response.VerifyEmailResponse;
import com.tap2eat.identity.dtos.request.ForgotPasswordRequest;
import com.tap2eat.identity.dtos.request.ResetPasswordRequest;
import com.tap2eat.identity.dtos.response.ForgotPasswordResponse;
import com.tap2eat.identity.dtos.response.ResetPasswordResponse;
import com.tap2eat.identity.dtos.request.ResendVerificationCodeRequest;
import com.tap2eat.identity.dtos.response.ResendVerificationCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tap2eat.identity.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.tap2eat.identity.dtos.request.UpdateProfileRequest;

@Tag(name = "Authentication", description = "Authentication and account management endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    @Autowired
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register account",
            description = "Creates a new account and sends an email verification code."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or invalid role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.registerAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Login",
            description = "Authenticates an account and returns access and refresh tokens."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Email not verified or account inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Get current account",
            description = "Returns the authenticated account and profile information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated account returned"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        MeResponse response = authService.getCurrentAccount(authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

        @Operation(
            summary = "Update current account profile",
            description = "Updates the authenticated account profile information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me")
    public ResponseEntity<MeResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        MeResponse response = authService.updateCurrentAccountProfile(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the provided refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Validates the refresh token and returns a new access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Verify email",
            description = "Verifies an account email using the verification code."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired verification code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        VerifyEmailResponse response = authService.verifyEmail(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Forgot password",
            description = "Generates and sends a password reset code if the email exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the account password using a valid reset code."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired reset code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid reset request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = authService.resetPassword(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Resend verification code",
            description = "Sends a new verification code if the account exists and is not yet verified."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/resend-verification-code")
    public ResponseEntity<ResendVerificationCodeResponse> resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequest request
    ) {
        ResendVerificationCodeResponse response = authService.resendVerificationCode(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}