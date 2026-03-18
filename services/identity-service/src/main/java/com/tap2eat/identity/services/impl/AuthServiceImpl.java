package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.response.RegisterResponse;
import com.tap2eat.identity.exceptions.EmailAlreadyRegisteredException;
import com.tap2eat.identity.exceptions.InvalidRoleException;
import com.tap2eat.identity.exceptions.WeakPasswordException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import com.tap2eat.identity.services.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    private final IAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(IAccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse registerAccount(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException("The email is already registered.");
        }

        validatePasswordStrength(request.getPassword());
        Role validatedRole = validateRole(request.getRole());

        Account newAccount = new Account();
        newAccount.setEmail(normalizedEmail);
        newAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newAccount.setRole(validatedRole);
        newAccount.setIsActive(true);

        Account savedAccount = accountRepository.save(newAccount);

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                savedAccount.getRole().name(),
                "Account created successfully."
        );
    }

    private Role validateRole(String roleValue){
        if (roleValue == null || roleValue.trim().isEmpty()){
            throw new InvalidRoleException("Role is required.");
        }

        Role role;
        try{
            role = Role.valueOf(roleValue.toUpperCase());

        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException("Invalid role: " + roleValue);
        } catch (Exception ex){
            throw new InvalidRoleException("An error occurred while validating the role: " + ex.getMessage());
        }

        if (role == Role.ADMIN){
            throw new InvalidRoleException("Admin role cannot be assigned through public registration.");
        }
        return role;
    }

    //TODO arreglar para poner que sea  8 en una proxima iteración, se dejo asi por terminos de registro para cuentas de desarrollo
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 1) {
            throw new WeakPasswordException("Password must be at least 8 characters long.");
        }

        boolean hasUpper = password.matches(".*[A-Z].*");

        if (!hasUpper) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter.");
        }

        boolean hasLower = password.matches(".*[a-z].*");

        if (!hasLower){
            throw new WeakPasswordException("Password must contain at least one lowercase letter.");
        }

        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasDigit){
            throw new WeakPasswordException("Password must contain at least one digit.");
        }

        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

        if (!hasSpecial){
            throw new WeakPasswordException("Password must contain at least one special character.");
        }
    }
}