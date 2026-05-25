package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.exceptions.InvalidEmailVerificationCodeException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.EmailVerificationCode;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IEmailVerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCodeServiceImplTest {

    @Mock
    private IEmailVerificationCodeRepository emailVerificationCodeRepository;

    private EmailVerificationCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        service = new EmailVerificationCodeServiceImpl(emailVerificationCodeRepository, messageSource);
        ReflectionTestUtils.setField(service, "emailVerificationCodeExpirationMinutes", 15L);
    }

    @Test
    void createCode_shouldMarkPreviousActiveCodesAsUsedAndCreateNewSixDigitCode() {
        Account account = account();
        EmailVerificationCode previousCode = code(account, "111111", LocalDateTime.now().plusMinutes(5), false);

        when(emailVerificationCodeRepository.findByAccount_IdAndUsedFalse(account.getId()))
                .thenReturn(List.of(previousCode));
        when(emailVerificationCodeRepository.save(any(EmailVerificationCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationCode newCode = service.createCode(account);

        assertTrue(previousCode.getUsed());
        verify(emailVerificationCodeRepository).saveAll(List.of(previousCode));
        assertSame(account, newCode.getAccount());
        assertFalse(newCode.getUsed());
        assertTrue(newCode.getCode().matches("\\d{6}"));
        assertTrue(newCode.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void validateCode_whenCodeExistsAndIsNotExpired_shouldReturnCode() {
        EmailVerificationCode verificationCode = code(account(), "123456", LocalDateTime.now().plusMinutes(1), false);
        when(emailVerificationCodeRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(verificationCode));

        assertSame(verificationCode, service.validateCode("123456"));
    }

    @Test
    void validateCode_whenCodeDoesNotExist_shouldThrowInvalidEmailVerificationCodeException() {
        when(emailVerificationCodeRepository.findByCodeAndUsedFalse("000000")).thenReturn(Optional.empty());

        assertThrows(InvalidEmailVerificationCodeException.class, () -> service.validateCode("000000"));
    }

    @Test
    void validateCode_whenCodeIsExpired_shouldThrowInvalidEmailVerificationCodeException() {
        EmailVerificationCode verificationCode = code(account(), "123456", LocalDateTime.now().minusSeconds(1), false);
        when(emailVerificationCodeRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(verificationCode));

        assertThrows(InvalidEmailVerificationCodeException.class, () -> service.validateCode("123456"));
    }

    @Test
    void markAsUsed_shouldPersistUsedCode() {
        EmailVerificationCode verificationCode = code(account(), "123456", LocalDateTime.now().plusMinutes(1), false);

        service.markAsUsed(verificationCode);

        assertTrue(verificationCode.getUsed());
        verify(emailVerificationCodeRepository).save(verificationCode);
    }

    private Account account() {
        Account account = new Account();
        account.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setEmail("user@example.com");
        account.setRole(Role.CUSTOMER);
        return account;
    }

    private EmailVerificationCode code(Account account, String value, LocalDateTime expiresAt, boolean used) {
        EmailVerificationCode code = new EmailVerificationCode();
        code.setAccount(account);
        code.setCode(value);
        code.setExpiresAt(expiresAt);
        code.setUsed(used);
        return code;
    }
}
