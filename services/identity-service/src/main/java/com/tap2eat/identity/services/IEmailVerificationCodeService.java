package com.tap2eat.identity.services;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.EmailVerificationCode;

public interface IEmailVerificationCodeService {
    EmailVerificationCode createCode(Account account);
    EmailVerificationCode validateCode(String code);
    void markAsUsed(EmailVerificationCode verificationCode);
}