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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    @Autowired
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.registerAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        MeResponse response = authService.getCurrentAccount(authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}