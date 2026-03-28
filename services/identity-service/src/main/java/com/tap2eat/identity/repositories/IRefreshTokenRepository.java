package com.tap2eat.identity.repositories;

import com.tap2eat.identity.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findFirstByAccount_IdAndRevokedFalseOrderByCreatedAtDesc(UUID accountId);

    void deleteByAccount_Id(UUID accountId);
}