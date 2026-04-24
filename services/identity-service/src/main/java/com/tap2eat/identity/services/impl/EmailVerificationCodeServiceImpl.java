package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.exceptions.InvalidEmailVerificationCodeException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.EmailVerificationCode;
import com.tap2eat.identity.repositories.IEmailVerificationCodeRepository;
import com.tap2eat.identity.services.IEmailVerificationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class EmailVerificationCodeServiceImpl implements IEmailVerificationCodeService {

    private final IEmailVerificationCodeRepository emailVerificationCodeRepository;
    private final MessageSource messageSource;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.email-verification-code.expiration-minutes}")
    private long emailVerificationCodeExpirationMinutes;

    public EmailVerificationCodeServiceImpl(IEmailVerificationCodeRepository emailVerificationCodeRepository,
                                            MessageSource messageSource) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.messageSource = messageSource;
    }

    @Override
    public EmailVerificationCode createCode(Account account) {
        List<EmailVerificationCode> activeCodes =
                emailVerificationCodeRepository.findByAccount_IdAndUsedFalse(account.getId());

        for (EmailVerificationCode code : activeCodes) {
            code.setUsed(true);
        }

        emailVerificationCodeRepository.saveAll(activeCodes);

        LocalDateTime now = LocalDateTime.now();

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setAccount(account);
        verificationCode.setCode(generateSixDigitCode());
        verificationCode.setExpiresAt(now.plusMinutes(emailVerificationCodeExpirationMinutes));
        verificationCode.setUsed(false);

        return emailVerificationCodeRepository.save(verificationCode);
    }

    @Override
    public EmailVerificationCode validateCode(String code) {
        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new InvalidEmailVerificationCodeException(
                        getMessage("auth.email.verification.code.invalid")
                ));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidEmailVerificationCodeException(
                    getMessage("auth.email.verification.code.expired")
            );
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

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}