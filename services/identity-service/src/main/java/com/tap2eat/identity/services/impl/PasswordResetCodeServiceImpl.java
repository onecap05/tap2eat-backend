package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.exceptions.InvalidPasswordResetCodeException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.PasswordResetCode;
import com.tap2eat.identity.repositories.IPasswordResetCodeRepository;
import com.tap2eat.identity.services.IPasswordResetCodeService;
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
public class PasswordResetCodeServiceImpl implements IPasswordResetCodeService {

    private final IPasswordResetCodeRepository passwordResetCodeRepository;
    private final MessageSource messageSource;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.password-reset-code.expiration-minutes}")
    private long passwordResetCodeExpirationMinutes;

    public PasswordResetCodeServiceImpl(IPasswordResetCodeRepository passwordResetCodeRepository,
                                        MessageSource messageSource) {
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.messageSource = messageSource;
    }

    @Override
    public PasswordResetCode createCode(Account account) {
        List<PasswordResetCode> activeCodes =
                passwordResetCodeRepository.findByAccount_IdAndUsedFalse(account.getId());

        for (PasswordResetCode code : activeCodes) {
            code.setUsed(true);
        }

        passwordResetCodeRepository.saveAll(activeCodes);

        LocalDateTime now = LocalDateTime.now();

        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setAccount(account);
        passwordResetCode.setCode(generateSixDigitCode());
        passwordResetCode.setExpiresAt(now.plusMinutes(passwordResetCodeExpirationMinutes));
        passwordResetCode.setUsed(false);

        return passwordResetCodeRepository.save(passwordResetCode);
    }

    @Override
    public PasswordResetCode validateCode(String code) {
        PasswordResetCode passwordResetCode = passwordResetCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new InvalidPasswordResetCodeException(
                        getMessage("auth.password.reset.code.invalid")
                ));

        if (passwordResetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidPasswordResetCodeException(
                    getMessage("auth.password.reset.code.expired")
            );
        }

        return passwordResetCode;
    }

    @Override
    public void markAsUsed(PasswordResetCode passwordResetCode) {
        passwordResetCode.setUsed(true);
        passwordResetCodeRepository.save(passwordResetCode);
    }

    private String generateSixDigitCode() {
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}