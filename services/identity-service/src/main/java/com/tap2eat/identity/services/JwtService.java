package com.tap2eat.identity.services;

import com.tap2eat.identity.config.PemUtils;
import com.tap2eat.identity.config.RsaKeyProperties;
import com.tap2eat.identity.models.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public JwtService(RsaKeyProperties rsaKeyProperties) throws Exception {
        this.privateKey = PemUtils.readPrivateKey(rsaKeyProperties.getPrivateKey());
        this.publicKey = PemUtils.readPublicKey(rsaKeyProperties.getPublicKey());
    }

    public String generateToken(Account account) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(account.getEmail())
                .claim("role", account.getRole().name())
                .claim("accountId", account.getId().toString())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, Account account) {
        final String username = extractUsername(token);
        return username.equals(account.getEmail()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}