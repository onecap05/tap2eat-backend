package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import com.tap2eat.identity.services.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    private final IAccountRepository accountRepository;

    @Autowired
    public AuthServiceImpl(IAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void registerAccount(RegisterRequest request) {
        // 1. Verify if the email is already in use
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("The email is already registered.");
        }

        // 2. Map request to Entity
        Account newAccount = new Account();
        newAccount.setEmail(request.getEmail());

        // TODO: Integrate Spring Security BCryptPasswordEncoder later
        newAccount.setPasswordHash(request.getPassword() + "_hashed");

        newAccount.setRole(Role.valueOf(request.getRole().toUpperCase()));

        // 3. Save to database
        accountRepository.save(newAccount);
    }
}