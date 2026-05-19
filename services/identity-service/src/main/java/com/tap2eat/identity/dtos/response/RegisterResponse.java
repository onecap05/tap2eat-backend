package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response returned after successful account registration")
public class RegisterResponse {

    @Schema(description = "Unique identifier of the created account", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Registered email address", example = "cliente1@ejemplo.com")
    private String email;

    @Schema(description = "Assigned account role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Operation result message", example = "Account created successfully. Please verify your email.")
    private String message;

    public RegisterResponse() {
    }

    public RegisterResponse(UUID id, String email, String role, String message) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}