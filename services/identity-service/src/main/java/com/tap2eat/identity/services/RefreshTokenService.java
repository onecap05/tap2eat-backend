package com.tap2eat.identity.services;

import com.tap2eat.identity.exceptions.InvalidRefreshTokenException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.RefreshToken;
import com.tap2eat.identity.repositories.IRefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final IRefreshTokenRepository refreshTokenRepository;
    private final MessageSource messageSource;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public RefreshTokenService(IRefreshTokenRepository refreshTokenRepository,
                               MessageSource messageSource) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.messageSource = messageSource;
    }

    public RefreshToken createRefreshToken(Account account) {
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setAccount(account);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plusSeconds(refreshTokenExpiration / 1000));

        return refreshTokenRepository.save(refreshToken);
    }

    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new InvalidRefreshTokenException(getMessage("auth.refresh.token.invalid")));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new InvalidRefreshTokenException(getMessage("auth.refresh.token.invalid")));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException(getMessage("auth.refresh.token.expired"));
        }

        return refreshToken;
    }

    public void deleteByAccountId(UUID accountId) {
        refreshTokenRepository.deleteByAccount_Id(accountId);
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}