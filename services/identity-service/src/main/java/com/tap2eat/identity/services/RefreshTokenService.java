package com.tap2eat.identity.services;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.RefreshToken;
import com.tap2eat.identity.repositories.IRefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final IRefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public RefreshTokenService(IRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(Account account) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setAccount(account);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));

        return refreshTokenRepository.save(refreshToken);
    }

    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or already revoked."));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}