package com.tap2eat.identity.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for account registration")
public class RegisterRequest {

    @Schema(
            description = "Account email address",
            example = "cliente1@ejemplo.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is incorrect")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(
            description = "Plain text password for the new account",
            example = "Tap2eat0722?",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 72, message = "Password must be between 1 and 72 characters")
    private String password;

    @Schema(
            description = "Publicly allowed role for registration",
            example = "CUSTOMER",
            allowableValues = {"CUSTOMER", "RESTAURANT_OWNER"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Role is required")
    private String role;

    @Schema(
            description = "First name of the account owner",
            example = "Angel",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Schema(
            description = "Last name of the account owner",
            example = "Ruiz",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Schema(
            description = "Optional phone number of the account owner",
            example = "2281234567"
    )
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    public RegisterRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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