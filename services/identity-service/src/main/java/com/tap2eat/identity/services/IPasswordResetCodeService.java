package com.tap2eat.identity.services;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.PasswordResetCode;

public interface IPasswordResetCodeService {
    PasswordResetCode createCode(Account account);
    PasswordResetCode validateCode(String code);
    void markAsUsed(PasswordResetCode passwordResetCode);
}