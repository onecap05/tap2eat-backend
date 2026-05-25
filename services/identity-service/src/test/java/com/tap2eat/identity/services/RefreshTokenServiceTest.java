package com.tap2eat.identity.services;

import com.tap2eat.identity.exceptions.InvalidRefreshTokenException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.RefreshToken;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IRefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, messageSource);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void createRefreshToken_shouldGenerateTokenForAccount() {
        Account account = account();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account);

        assertNotNull(refreshToken.getId());
        assertNotNull(refreshToken.getToken());
        assertSame(account, refreshToken.getAccount());
        assertFalse(refreshToken.getRevoked());
        assertNotNull(refreshToken.getCreatedAt());
        assertTrue(refreshToken.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
    }

    @Test
    void revokeToken_whenTokenExists_shouldMarkItRevoked() {
        RefreshToken refreshToken = refreshToken(account(), "refresh-token", LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeToken("refresh-token");

        assertTrue(refreshToken.getRevoked());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeToken_whenTokenDoesNotExist_shouldThrowInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("missing-token")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.revokeToken("missing-token"));
    }

    @Test
    void validateRefreshToken_whenTokenIsValid_shouldReturnToken() {
        RefreshToken refreshToken = refreshToken(account(), "refresh-token", LocalDateTime.now().plusMinutes(5));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertSame(refreshToken, refreshTokenService.validateRefreshToken("refresh-token"));
    }

    @Test
    void validateRefreshToken_whenTokenExpired_shouldThrowInvalidRefreshTokenException() {
        RefreshToken refreshToken = refreshToken(account(), "refresh-token", LocalDateTime.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.validateRefreshToken("refresh-token"));
    }

    @Test
    void deleteByAccountId_shouldDelegateToRepository() {
        refreshTokenService.deleteByAccountId(ACCOUNT_ID);

        ArgumentCaptor<UUID> accountIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(refreshTokenRepository).deleteByAccount_Id(accountIdCaptor.capture());
        assertEquals(ACCOUNT_ID, accountIdCaptor.getValue());
    }

    private Account account() {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setEmail("user@example.com");
        account.setRole(Role.CUSTOMER);
        account.setIsActive(true);
        account.setEmailVerified(true);
        return account;
    }

    private RefreshToken refreshToken(Account account, String token, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccount(account);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);
        return refreshToken;
    }
}
