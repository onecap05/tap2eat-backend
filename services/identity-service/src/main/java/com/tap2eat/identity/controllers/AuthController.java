package com.tap2eat.identity.controllers;

import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.services.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    @Autowired
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.registerAccount(request);
        return new ResponseEntity<>("Account created successfully", HttpStatus.CREATED);
    }
}