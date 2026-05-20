package com.tap2eat.identity.config;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.AccountProfile;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthTestDataSeeder {

    @Bean
    public CommandLineRunner seedAuthTestUser(
            IAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.test-seed.enabled:false}") boolean enabled,
            @Value("${auth.test-seed.email:}") String email,
            @Value("${auth.test-seed.password:}") String password,
            @Value("${auth.test-seed.role:CUSTOMER}") String role,
            @Value("${auth.test-seed.first-name:CI}") String firstName,
            @Value("${auth.test-seed.last-name:User}") String lastName,
            @Value("${auth.test-seed.phone:}") String phone
    ) {
        return args -> {
            if (!enabled) {
                return;
            }

            if (email == null || email.isBlank()) {
                throw new IllegalStateException("auth.test-seed.email is required when auth.test-seed.enabled=true");
            }

            if (password == null || password.isBlank()) {
                throw new IllegalStateException("auth.test-seed.password is required when auth.test-seed.enabled=true");
            }

            Role parsedRole;
            try {
                parsedRole = Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Invalid auth.test-seed.role value: " + role);
            }

            String normalizedEmail = email.trim().toLowerCase();

            Account account = accountRepository.findByEmail(normalizedEmail).orElseGet(Account::new);

            account.setEmail(normalizedEmail);
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setRole(parsedRole);
            account.setIsActive(true);
            account.setEmailVerified(true);

            AccountProfile profile = account.getProfile();
            if (profile == null) {
                profile = new AccountProfile();
                account.setProfile(profile);
            }

            profile.setFirstName(firstName.trim());
            profile.setLastName(lastName.trim());
            profile.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);

            accountRepository.save(account);
        };
    }
}