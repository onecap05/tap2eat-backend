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

    public RefreshToken createRefreshToken(Account account, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setToken(token);
        refreshToken.setAccount(account);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));

        return refreshTokenRepository.save(refreshToken);
    }
}