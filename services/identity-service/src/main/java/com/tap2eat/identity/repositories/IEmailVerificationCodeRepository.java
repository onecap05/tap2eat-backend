package com.tap2eat.identity.repositories;

import com.tap2eat.identity.models.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findByCodeAndUsedFalse(String code);

    Optional<EmailVerificationCode> findFirstByAccount_IdAndUsedFalseOrderByCreatedAtDesc(UUID accountId);

    List<EmailVerificationCode> findByAccount_IdAndUsedFalse(UUID accountId);
}