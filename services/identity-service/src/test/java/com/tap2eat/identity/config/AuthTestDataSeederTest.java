package com.tap2eat.identity.config;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.AccountProfile;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTestDataSeederTest {

    @Mock
    private IAccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedAuthTestUser_whenDisabled_shouldDoNothing() throws Exception {
        CommandLineRunner runner = new AuthTestDataSeeder().seedAuthTestUser(
                accountRepository,
                passwordEncoder,
                false,
                "user@example.com",
                "Strong123!",
                "CUSTOMER",
                "Angel",
                "Ruiz",
                "2281234567"
        );

        runner.run();

        verifyNoInteractions(accountRepository, passwordEncoder);
    }

    @Test
    void seedAuthTestUser_whenEnabledAndAccountDoesNotExist_shouldCreateVerifiedActiveAccount() throws Exception {
        when(accountRepository.findByEmail("seed@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Strong123!")).thenReturn("encoded-password");

        CommandLineRunner runner = new AuthTestDataSeeder().seedAuthTestUser(
                accountRepository,
                passwordEncoder,
                true,
                " Seed@Example.com ",
                "Strong123!",
                "customer",
                " Angel ",
                " Ruiz ",
                " 2281234567 "
        );

        runner.run();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals("seed@example.com", savedAccount.getEmail());
        assertEquals("encoded-password", savedAccount.getPasswordHash());
        assertEquals(Role.CUSTOMER, savedAccount.getRole());
        assertTrue(savedAccount.getIsActive());
        assertTrue(savedAccount.getEmailVerified());
        assertEquals("Angel", savedAccount.getProfile().getFirstName());
        assertEquals("Ruiz", savedAccount.getProfile().getLastName());
        assertEquals("2281234567", savedAccount.getProfile().getPhone());
    }

    @Test
    void seedAuthTestUser_whenAccountExists_shouldUpdateExistingProfileAndAllowBlankPhone() throws Exception {
        Account existingAccount = new Account();
        AccountProfile profile = new AccountProfile();
        existingAccount.setProfile(profile);
        when(accountRepository.findByEmail("seed@example.com")).thenReturn(Optional.of(existingAccount));
        when(passwordEncoder.encode("Strong123!")).thenReturn("encoded-password");

        CommandLineRunner runner = new AuthTestDataSeeder().seedAuthTestUser(
                accountRepository,
                passwordEncoder,
                true,
                "seed@example.com",
                "Strong123!",
                "RESTAURANT_OWNER",
                "Owner",
                "User",
                " "
        );

        runner.run();

        verify(accountRepository).save(existingAccount);
        assertEquals("seed@example.com", existingAccount.getEmail());
        assertEquals(Role.RESTAURANT_OWNER, existingAccount.getRole());
        assertEquals("Owner", existingAccount.getProfile().getFirstName());
        assertEquals("User", existingAccount.getProfile().getLastName());
        assertNull(existingAccount.getProfile().getPhone());
    }

    @Test
    void seedAuthTestUser_whenRequiredValuesAreMissing_shouldThrowIllegalStateException() {
        AuthTestDataSeeder seeder = new AuthTestDataSeeder();

        assertThrows(IllegalStateException.class, () -> seeder.seedAuthTestUser(
                accountRepository, passwordEncoder, true, " ", "Strong123!", "CUSTOMER", "A", "B", null
        ).run());

        assertThrows(IllegalStateException.class, () -> seeder.seedAuthTestUser(
                accountRepository, passwordEncoder, true, "user@example.com", " ", "CUSTOMER", "A", "B", null
        ).run());

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void seedAuthTestUser_whenRoleIsInvalid_shouldThrowIllegalStateException() {
        CommandLineRunner runner = new AuthTestDataSeeder().seedAuthTestUser(
                accountRepository,
                passwordEncoder,
                true,
                "user@example.com",
                "Strong123!",
                "INVALID",
                "A",
                "B",
                null
        );

        assertThrows(IllegalStateException.class, runner::run);
    }
}
