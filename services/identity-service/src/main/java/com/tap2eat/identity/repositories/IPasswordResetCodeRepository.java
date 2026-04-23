package com.tap2eat.identity.repositories;

import com.tap2eat.identity.models.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IPasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    Optional<PasswordResetCode> findByCodeAndUsedFalse(String code);

    Optional<PasswordResetCode> findFirstByAccount_IdAndUsedFalseOrderByCreatedAtDesc(UUID accountId);

    List<PasswordResetCode> findByAccount_IdAndUsedFalse(UUID accountId);
}