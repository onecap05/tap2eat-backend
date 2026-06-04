package com.tap2eat.identity.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    @Test
    void account_shouldExposeFieldsAndKeepProfileRelationshipInSync() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        Account account = new Account();
        AccountProfile profile = new AccountProfile();

        account.setId(id);
        account.setEmail("user@example.com");
        account.setPasswordHash("hash");
        account.setRole(Role.CUSTOMER);
        account.setIsActive(false);
        account.setCreatedAt(createdAt);
        account.setUpdatedAt(updatedAt);
        account.setEmailVerified(true);
        account.setProfile(profile);

        assertEquals(id, account.getId());
        assertEquals("user@example.com", account.getEmail());
        assertEquals("hash", account.getPasswordHash());
        assertEquals(Role.CUSTOMER, account.getRole());
        assertFalse(account.getIsActive());
        assertEquals(createdAt, account.getCreatedAt());
        assertEquals(updatedAt, account.getUpdatedAt());
        assertTrue(account.getEmailVerified());
        assertSame(profile, account.getProfile());
        assertSame(account, profile.getAccount());
    }

    @Test
    void accountLifecycleCallbacks_shouldSetTimestampsAndDefaultEmailVerified() {
        Account account = new Account();
        account.setEmailVerified(null);

        account.onCreate();

        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());
        assertFalse(account.getEmailVerified());

        LocalDateTime createdAt = account.getCreatedAt();
        account.onUpdate();

        assertEquals(createdAt, account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());
    }

    @Test
    void accountProfile_shouldExposeFieldsAndLifecycleTimestamps() {
        UUID id = UUID.randomUUID();
        Account account = new Account();
        AccountProfile profile = new AccountProfile();

        profile.setId(id);
        profile.setAccount(account);
        profile.setFirstName("Angel");
        profile.setLastName("Ruiz");
        profile.setPhone("2281234567");
        profile.onCreate();

        assertEquals(id, profile.getId());
        assertSame(account, profile.getAccount());
        assertEquals("Angel", profile.getFirstName());
        assertEquals("Ruiz", profile.getLastName());
        assertEquals("2281234567", profile.getPhone());
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());

        LocalDateTime createdAt = profile.getCreatedAt();
        profile.onUpdate();
        assertEquals(createdAt, profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
    }

    @Test
    void emailVerificationCode_shouldExposeFieldsAndLifecycleDefaults() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        LocalDateTime createdAt = LocalDateTime.now();
        Account account = new Account();
        EmailVerificationCode code = new EmailVerificationCode();

        code.setId(id);
        code.setCode("123456");
        code.setExpiresAt(expiresAt);
        code.setUsed(true);
        code.setCreatedAt(createdAt);
        code.setAccount(account);

        assertEquals(id, code.getId());
        assertEquals("123456", code.getCode());
        assertEquals(expiresAt, code.getExpiresAt());
        assertTrue(code.getUsed());
        assertEquals(createdAt, code.getCreatedAt());
        assertSame(account, code.getAccount());

        code.setUsed(null);
        code.onCreate();
        assertFalse(code.getUsed());
        assertNotNull(code.getCreatedAt());
    }

    @Test
    void passwordResetCode_shouldExposeFieldsAndLifecycleDefaults() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        LocalDateTime createdAt = LocalDateTime.now();
        Account account = new Account();
        PasswordResetCode code = new PasswordResetCode();

        code.setId(id);
        code.setCode("654321");
        code.setExpiresAt(expiresAt);
        code.setUsed(true);
        code.setCreatedAt(createdAt);
        code.setAccount(account);

        assertEquals(id, code.getId());
        assertEquals("654321", code.getCode());
        assertEquals(expiresAt, code.getExpiresAt());
        assertTrue(code.getUsed());
        assertEquals(createdAt, code.getCreatedAt());
        assertSame(account, code.getAccount());

        code.setUsed(null);
        code.onCreate();
        assertFalse(code.getUsed());
        assertNotNull(code.getCreatedAt());
    }

    @Test
    void refreshToken_shouldExposeFieldsAndLifecycleDefaults() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        LocalDateTime createdAt = LocalDateTime.now();
        Account account = new Account();
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setId(id);
        refreshToken.setToken("refresh-token");
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(true);
        refreshToken.setCreatedAt(createdAt);
        refreshToken.setAccount(account);

        assertEquals(id, refreshToken.getId());
        assertEquals("refresh-token", refreshToken.getToken());
        assertEquals(expiresAt, refreshToken.getExpiresAt());
        assertTrue(refreshToken.getRevoked());
        assertEquals(createdAt, refreshToken.getCreatedAt());
        assertSame(account, refreshToken.getAccount());

        RefreshToken defaultRefreshToken = new RefreshToken();
        defaultRefreshToken.setRevoked(null);
        defaultRefreshToken.prePersist();

        assertNotNull(defaultRefreshToken.getId());
        assertNotNull(defaultRefreshToken.getCreatedAt());
        assertFalse(defaultRefreshToken.getRevoked());
    }
}
