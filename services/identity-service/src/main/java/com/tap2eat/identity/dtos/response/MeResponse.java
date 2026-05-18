package com.tap2eat.identity.dtos.response;

import java.util.UUID;

public class MeResponse {

    private UUID id;
    private String email;
    private String role;
    private Boolean isActive;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
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