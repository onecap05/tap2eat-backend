package com.tap2eat.identity.dtos.response;

import java.util.UUID;

public class LoginResponse {

    private UUID id;
    private String email;
    private String role;
    private String token;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(UUID id, String email, String role, String token, String message) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.token = token;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}