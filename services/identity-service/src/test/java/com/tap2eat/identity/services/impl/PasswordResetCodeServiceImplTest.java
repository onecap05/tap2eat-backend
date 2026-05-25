package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.exceptions.InvalidPasswordResetCodeException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.PasswordResetCode;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IPasswordResetCodeRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetCodeServiceImplTest {

    @Mock
    private IPasswordResetCodeRepository passwordResetCodeRepository;

    private PasswordResetCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        service = new PasswordResetCodeServiceImpl(passwordResetCodeRepository, messageSource);
        ReflectionTestUtils.setField(service, "passwordResetCodeExpirationMinutes", 15L);
    }

    @Test
    void createCode_shouldMarkPreviousActiveCodesAsUsedAndCreateNewSixDigitCode() {
        Account account = account();
        PasswordResetCode previousCode = code(account, "111111", LocalDateTime.now().plusMinutes(5), false);

        when(passwordResetCodeRepository.findByAccount_IdAndUsedFalse(account.getId()))
                .thenReturn(List.of(previousCode));
        when(passwordResetCodeRepository.save(any(PasswordResetCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetCode newCode = service.createCode(account);

        assertTrue(previousCode.getUsed());
        verify(passwordResetCodeRepository).saveAll(List.of(previousCode));
        assertSame(account, newCode.getAccount());
        assertFalse(newCode.getUsed());
        assertTrue(newCode.getCode().matches("\\d{6}"));
        assertTrue(newCode.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void validateCode_whenCodeExistsAndIsNotExpired_shouldReturnCode() {
        PasswordResetCode resetCode = code(account(), "123456", LocalDateTime.now().plusMinutes(1), false);
        when(passwordResetCodeRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(resetCode));

        assertSame(resetCode, service.validateCode("123456"));
    }

    @Test
    void validateCode_whenCodeDoesNotExist_shouldThrowInvalidPasswordResetCodeException() {
        when(passwordResetCodeRepository.findByCodeAndUsedFalse("000000")).thenReturn(Optional.empty());

        assertThrows(InvalidPasswordResetCodeException.class, () -> service.validateCode("000000"));
    }

    @Test
    void validateCode_whenCodeIsExpired_shouldThrowInvalidPasswordResetCodeException() {
        PasswordResetCode resetCode = code(account(), "123456", LocalDateTime.now().minusSeconds(1), false);
        when(passwordResetCodeRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(resetCode));

        assertThrows(InvalidPasswordResetCodeException.class, () -> service.validateCode("123456"));
    }

    @Test
    void markAsUsed_shouldPersistUsedCode() {
        PasswordResetCode resetCode = code(account(), "123456", LocalDateTime.now().plusMinutes(1), false);

        service.markAsUsed(resetCode);

        assertTrue(resetCode.getUsed());
        verify(passwordResetCodeRepository).save(resetCode);
    }

    private Account account() {
        Account account = new Account();
        account.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setEmail("user@example.com");
        account.setRole(Role.CUSTOMER);
        return account;
    }

    private PasswordResetCode code(Account account, String value, LocalDateTime expiresAt, boolean used) {
        PasswordResetCode code = new PasswordResetCode();
        code.setAccount(account);
        code.setCode(value);
        code.setExpiresAt(expiresAt);
        code.setUsed(used);
        return code;
    }
}
