package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.EmailVerificationCode;
import com.tap2eat.identity.repositories.IEmailVerificationCodeRepository;
import com.tap2eat.identity.services.IEmailVerificationCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.security.SecureRandom;

@Service
@Transactional
public class EmailVerificationCodeServiceImpl implements IEmailVerificationCodeService {

    private final IEmailVerificationCodeRepository emailVerificationCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationCodeServiceImpl(IEmailVerificationCodeRepository emailVerificationCodeRepository) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
    }

    @Override
    public EmailVerificationCode createCode(Account account) {
        List<EmailVerificationCode> activeCodes =
                emailVerificationCodeRepository.findByAccount_IdAndUsedFalse(account.getId());

        for (EmailVerificationCode code : activeCodes) {
            code.setUsed(true);
        }

        emailVerificationCodeRepository.saveAll(activeCodes);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setAccount(account);
        verificationCode.setCode(generateSixDigitCode());
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        verificationCode.setUsed(false);

        return emailVerificationCodeRepository.save(verificationCode);
    }

    @Override
    public EmailVerificationCode validateCode(String code) {
        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new RuntimeException("Verification code not found or already used."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code has expired.");
        }

        return verificationCode;
    }

    @Override
    public void markAsUsed(EmailVerificationCode verificationCode) {
        verificationCode.setUsed(true);
        emailVerificationCodeRepository.save(verificationCode);
    }

    private String generateSixDigitCode() {
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }
}