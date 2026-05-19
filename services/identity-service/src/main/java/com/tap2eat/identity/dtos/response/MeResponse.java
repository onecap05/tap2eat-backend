package com.tap2eat.identity.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Authenticated account and profile information")
public class MeResponse {

    @Schema(description = "Unique identifier of the authenticated account", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Authenticated email address", example = "cliente1@ejemplo.com")
    private String email;

    @Schema(description = "Assigned account role", example = "CUSTOMER")
    private String role;

    @Schema(description = "Indicates whether the account is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Indicates whether the account email has been verified", example = "true")
    private Boolean emailVerified;

    @Schema(description = "First name of the account owner", example = "Angel")
    private String firstName;

    @Schema(description = "Last name of the account owner", example = "Ruiz")
    private String lastName;

    @Schema(description = "Phone number of the account owner", example = "2281234567")
    private String phone;

    public MeResponse() {
    }

    public MeResponse(UUID id,
                      String email,
                      String role,
                      Boolean isActive,
                      Boolean emailVerified,
                      String firstName,
                      String lastName,
                      String phone) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.emailVerified = emailVerified;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}