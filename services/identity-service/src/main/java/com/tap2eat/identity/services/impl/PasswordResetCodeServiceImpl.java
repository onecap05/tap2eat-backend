package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.PasswordResetCode;
import com.tap2eat.identity.repositories.IPasswordResetCodeRepository;
import com.tap2eat.identity.services.IPasswordResetCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PasswordResetCodeServiceImpl implements IPasswordResetCodeService {

    private final IPasswordResetCodeRepository passwordResetCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetCodeServiceImpl(IPasswordResetCodeRepository passwordResetCodeRepository) {
        this.passwordResetCodeRepository = passwordResetCodeRepository;
    }

    @Override
    public PasswordResetCode createCode(Account account) {
        List<PasswordResetCode> activeCodes =
                passwordResetCodeRepository.findByAccount_IdAndUsedFalse(account.getId());

        for (PasswordResetCode code : activeCodes) {
            code.setUsed(true);
        }

        passwordResetCodeRepository.saveAll(activeCodes);

        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setAccount(account);
        passwordResetCode.setCode(generateSixDigitCode());
        passwordResetCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetCode.setUsed(false);

        return passwordResetCodeRepository.save(passwordResetCode);
    }

    @Override
    public PasswordResetCode validateCode(String code) {
        PasswordResetCode passwordResetCode = passwordResetCodeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new RuntimeException("Password reset code not found or already used."));

        if (passwordResetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset code has expired.");
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
}