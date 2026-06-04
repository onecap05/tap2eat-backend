package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.dtos.request.ForgotPasswordRequest;
import com.tap2eat.identity.dtos.request.LoginRequest;
import com.tap2eat.identity.dtos.request.LogoutRequest;
import com.tap2eat.identity.dtos.request.RefreshTokenRequest;
import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.request.ResendVerificationCodeRequest;
import com.tap2eat.identity.dtos.request.ResetPasswordRequest;
import com.tap2eat.identity.dtos.request.VerifyEmailRequest;
import com.tap2eat.identity.dtos.response.LoginResponse;
import com.tap2eat.identity.dtos.response.MeResponse;
import com.tap2eat.identity.dtos.response.RegisterResponse;
import com.tap2eat.identity.dtos.response.TokenRefreshResponse;
import com.tap2eat.identity.exceptions.EmailAlreadyRegisteredException;
import com.tap2eat.identity.exceptions.EmailNotVerifiedException;
import com.tap2eat.identity.exceptions.InactiveAccountException;
import com.tap2eat.identity.exceptions.InvalidCredentialsException;
import com.tap2eat.identity.exceptions.InvalidRoleException;
import com.tap2eat.identity.exceptions.WeakPasswordException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.AccountProfile;
import com.tap2eat.identity.models.PasswordResetCode;
import com.tap2eat.identity.models.RefreshToken;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import com.tap2eat.identity.services.IEmailVerificationCodeService;
import com.tap2eat.identity.services.IPasswordResetCodeService;
import com.tap2eat.identity.services.JwtService;
import com.tap2eat.identity.services.NotificationGrpcClient;
import com.tap2eat.identity.services.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IAccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private IEmailVerificationCodeService emailVerificationCodeService;

    @Mock
    private NotificationGrpcClient notificationGrpcClient;

    @Mock
    private IPasswordResetCodeService passwordResetCodeService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);

        authService = new AuthServiceImpl(
                accountRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                emailVerificationCodeService,
                notificationGrpcClient,
                passwordResetCodeService,
                messageSource
        );
        ReflectionTestUtils.setField(authService, "passwordMinLength", 8);
    }

    @Test
    void registerAccount_shouldCreatePendingRegistrationAndSendVerificationCode() {
        RegisterRequest request = registerRequest("  New.User@Example.com  ", "Strong123!", "CUSTOMER");

        when(accountRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Strong123!")).thenReturn("encoded-password");

        RegisterResponse response = authService.registerAccount(request);

        assertNull(response.getId());
        assertEquals("new.user@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("auth.account.created.verify", response.getMessage());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationGrpcClient).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq("new.user@example.com"),
                codeCaptor.capture()
        );
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void registerAccount_whenEmailAlreadyExists_shouldThrowConflictException() {
        RegisterRequest request = registerRequest("Used@Example.com", "Strong123!", "CUSTOMER");
        when(accountRepository.existsByEmail("used@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.registerAccount(request));

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(notificationGrpcClient);
    }

    @Test
    void registerAccount_whenRoleIsBlankOrInvalidOrAdmin_shouldThrowInvalidRoleException() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(InvalidRoleException.class,
                () -> authService.registerAccount(registerRequest("blank@example.com", "Strong123!", " ")));
        assertThrows(InvalidRoleException.class,
                () -> authService.registerAccount(registerRequest("invalid@example.com", "Strong123!", "OWNERISH")));
        assertThrows(InvalidRoleException.class,
                () -> authService.registerAccount(registerRequest("admin@example.com", "Strong123!", "ADMIN")));

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(notificationGrpcClient);
    }

    @Test
    void registerAccount_whenPasswordIsWeak_shouldThrowWeakPasswordExceptionForEachRule() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(WeakPasswordException.class,
                () -> authService.registerAccount(registerRequest("short@example.com", "S1!", "CUSTOMER")));
        assertThrows(WeakPasswordException.class,
                () -> authService.registerAccount(registerRequest("upper@example.com", "strong123!", "CUSTOMER")));
        assertThrows(WeakPasswordException.class,
                () -> authService.registerAccount(registerRequest("lower@example.com", "STRONG123!", "CUSTOMER")));
        assertThrows(WeakPasswordException.class,
                () -> authService.registerAccount(registerRequest("digit@example.com", "StrongPass!", "CUSTOMER")));
        assertThrows(WeakPasswordException.class,
                () -> authService.registerAccount(registerRequest("special@example.com", "Strong123", "CUSTOMER")));

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(notificationGrpcClient);
    }

    @Test
    void verifyEmail_whenPendingCodeIsValid_shouldPersistVerifiedAccountAndProfile() {
        String verificationCode = registerPendingAccount("new.user@example.com", "Strong123!", "CUSTOMER");
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setCode(verificationCode);

        when(accountRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(ACCOUNT_ID);
            return account;
        });

        assertEquals("auth.email.verified.success", authService.verifyEmail(request).getMessage());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals("new.user@example.com", savedAccount.getEmail());
        assertEquals("encoded-password", savedAccount.getPasswordHash());
        assertEquals(Role.CUSTOMER, savedAccount.getRole());
        assertTrue(savedAccount.getEmailVerified());
        assertTrue(savedAccount.getIsActive());
        assertNotNull(savedAccount.getProfile());
        assertEquals("Angel", savedAccount.getProfile().getFirstName());
        assertEquals("Ruiz", savedAccount.getProfile().getLastName());
        assertEquals("2281234567", savedAccount.getProfile().getPhone());
        assertEquals(savedAccount, savedAccount.getProfile().getAccount());
    }

    @Test
    void verifyEmail_whenCodeDoesNotExist_shouldThrowInvalidCredentialsException() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setCode("000000");

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyEmail(request));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void verifyEmail_whenEmailWasRegisteredBeforeVerification_shouldThrowConflictException() {
        when(accountRepository.existsByEmail("new.user@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode("Strong123!")).thenReturn("encoded-password");

        RegisterRequest registerRequest = registerRequest("new.user@example.com", "Strong123!", "CUSTOMER");
        authService.registerAccount(registerRequest);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationGrpcClient).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq("new.user@example.com"),
                codeCaptor.capture()
        );

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setCode(codeCaptor.getValue());

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.verifyEmail(request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void login_whenCredentialsAreValid_shouldReturnJwtAndRefreshToken() {
        LoginRequest request = loginRequest("  USER@Example.com ", "Strong123!");
        Account account = account("user@example.com", true, true);
        RefreshToken refreshToken = refreshToken(account, "refresh-token");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Strong123!", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("jwt-token");
        when(jwtService.getJwtExpiration()).thenReturn(120000L);
        when(refreshTokenService.createRefreshToken(account)).thenReturn(refreshToken);

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(120000L, response.getExpiresIn());
    }

    @Test
    void login_whenAccountDoesNotExist_shouldThrowInvalidCredentialsException() {
        LoginRequest request = loginRequest("missing@example.com", "Strong123!");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_whenPasswordDoesNotMatch_shouldThrowInvalidCredentialsException() {
        LoginRequest request = loginRequest("user@example.com", "Wrong123!");
        Account account = account("user@example.com", true, true);

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Wrong123!", "encoded-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void login_whenEmailIsNotVerified_shouldThrowEmailNotVerifiedException() {
        LoginRequest request = loginRequest("user@example.com", "Strong123!");
        Account account = account("user@example.com", true, false);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(request));

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_whenAccountIsInactive_shouldThrowInactiveAccountException() {
        LoginRequest request = loginRequest("user@example.com", "Strong123!");
        Account account = account("user@example.com", false, true);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        assertThrows(InactiveAccountException.class, () -> authService.login(request));

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void refreshToken_whenRefreshTokenIsValid_shouldReturnNewAccessToken() {
        Account account = account("user@example.com", true, true);
        RefreshToken refreshToken = refreshToken(account, "refresh-token");
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenService.validateRefreshToken("refresh-token")).thenReturn(refreshToken);
        when(jwtService.generateToken(account)).thenReturn("new-jwt-token");

        TokenRefreshResponse response = authService.refreshToken(request);

        assertEquals("new-jwt-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");

        authService.logout(request);

        verify(refreshTokenService).revokeToken("refresh-token");
    }

    @Test
    void forgotPassword_whenAccountExists_shouldCreateCodeAndSendEmail() {
        Account account = account("user@example.com", true, true);
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setCode("654321");
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(" USER@example.com ");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordResetCodeService.createCode(account)).thenReturn(resetCode);

        assertEquals("auth.forgot.password.sent", authService.forgotPassword(request).getMessage());
        verify(notificationGrpcClient).sendVerificationEmail("user@example.com", "654321");
    }

    @Test
    void forgotPassword_whenAccountDoesNotExist_shouldReturnGenericMessageWithoutSendingEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertEquals("auth.forgot.password.sent", authService.forgotPassword(request).getMessage());
        verifyNoInteractions(passwordResetCodeService, notificationGrpcClient);
    }

    @Test
    void resetPassword_whenCodeMatchesAccount_shouldChangePasswordAndRevokeSessions() {
        Account account = account("user@example.com", true, true);
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setAccount(account);
        ResetPasswordRequest request = resetPasswordRequest(" USER@example.com ", "654321", "NewStrong123!");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordResetCodeService.validateCode("654321")).thenReturn(resetCode);
        when(passwordEncoder.encode("NewStrong123!")).thenReturn("new-hash");

        assertEquals("auth.password.reset.success", authService.resetPassword(request).getMessage());
        assertEquals("new-hash", account.getPasswordHash());
        verify(accountRepository).save(account);
        verify(passwordResetCodeService).markAsUsed(resetCode);
        verify(refreshTokenService).deleteByAccountId(ACCOUNT_ID);
    }

    @Test
    void resetPassword_whenAccountDoesNotExist_shouldThrowInvalidCredentialsException() {
        ResetPasswordRequest request = resetPasswordRequest("missing@example.com", "654321", "NewStrong123!");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPassword(request));

        verifyNoInteractions(passwordResetCodeService, passwordEncoder, refreshTokenService);
    }

    @Test
    void resetPassword_whenCodeBelongsToAnotherAccount_shouldThrowInvalidCredentialsException() {
        Account account = account("user@example.com", true, true);
        Account otherAccount = account("other@example.com", true, true);
        otherAccount.setId(OTHER_ACCOUNT_ID);
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setAccount(otherAccount);
        ResetPasswordRequest request = resetPasswordRequest("user@example.com", "654321", "NewStrong123!");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordResetCodeService.validateCode("654321")).thenReturn(resetCode);

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPassword(request));

        verifyNoInteractions(passwordEncoder);
        verify(accountRepository, never()).save(any(Account.class));
        verify(passwordResetCodeService, never()).markAsUsed(any(PasswordResetCode.class));
        verify(refreshTokenService, never()).deleteByAccountId(any(UUID.class));
    }

    @Test
    void resetPassword_whenNewPasswordIsWeak_shouldThrowWeakPasswordException() {
        Account account = account("user@example.com", true, true);
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setAccount(account);
        ResetPasswordRequest request = resetPasswordRequest("user@example.com", "654321", "weakpass");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordResetCodeService.validateCode("654321")).thenReturn(resetCode);

        assertThrows(WeakPasswordException.class, () -> authService.resetPassword(request));

        verifyNoInteractions(passwordEncoder);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getCurrentAccount_whenAccountExists_shouldReturnAccountAndProfile() {
        Account account = account("user@example.com", true, true);
        AccountProfile profile = new AccountProfile();
        profile.setFirstName("Angel");
        profile.setLastName("Ruiz");
        profile.setPhone("2281234567");
        account.setProfile(profile);

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        MeResponse response = authService.getCurrentAccount(" USER@example.com ");

        assertEquals(ACCOUNT_ID, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertTrue(response.getIsActive());
        assertTrue(response.getEmailVerified());
        assertEquals("Angel", response.getFirstName());
        assertEquals("Ruiz", response.getLastName());
        assertEquals("2281234567", response.getPhone());
    }

    @Test
    void getCurrentAccount_whenAccountHasNoProfile_shouldReturnNullProfileFields() {
        Account account = account("user@example.com", true, true);
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        MeResponse response = authService.getCurrentAccount("user@example.com");

        assertNull(response.getFirstName());
        assertNull(response.getLastName());
        assertNull(response.getPhone());
    }

    @Test
    void getCurrentAccount_whenAccountDoesNotExist_shouldThrowInvalidCredentialsException() {
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentAccount("missing@example.com"));
    }

    @Test
    void resendVerificationCode_whenAccountIsAlreadyVerified_shouldReturnAlreadyVerifiedMessage() {
        Account account = account("user@example.com", true, true);
        ResendVerificationCodeRequest request = new ResendVerificationCodeRequest();
        request.setEmail("user@example.com");

        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        assertEquals("auth.email.already.verified", authService.resendVerificationCode(request).getMessage());
        verifyNoInteractions(emailVerificationCodeService, notificationGrpcClient);
    }

    @Test
    void resendVerificationCode_whenNoPendingRegistrationExists_shouldReturnGenericMessageWithoutSendingEmail() {
        ResendVerificationCodeRequest request = new ResendVerificationCodeRequest();
        request.setEmail("missing@example.com");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertEquals("auth.verification.code.resent", authService.resendVerificationCode(request).getMessage());
        verifyNoInteractions(notificationGrpcClient);
    }

    @Test
    void resendVerificationCode_whenPendingRegistrationExists_shouldGenerateAndSendNewCode() {
        registerPendingAccount("new.user@example.com", "Strong123!", "CUSTOMER");
        ResendVerificationCodeRequest request = new ResendVerificationCodeRequest();
        request.setEmail(" new.user@example.com ");

        when(accountRepository.findByEmail("new.user@example.com")).thenReturn(Optional.empty());

        assertEquals("auth.verification.code.resent", authService.resendVerificationCode(request).getMessage());

        verify(notificationGrpcClient, org.mockito.Mockito.times(2))
                .sendVerificationEmail(org.mockito.ArgumentMatchers.eq("new.user@example.com"), anyString());
    }

    private String registerPendingAccount(String email, String password, String role) {
        when(accountRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded-password");

        authService.registerAccount(registerRequest(email, password, role));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationGrpcClient).sendVerificationEmail(
                org.mockito.ArgumentMatchers.eq(email),
                codeCaptor.capture()
        );
        return codeCaptor.getValue();
    }

    private RegisterRequest registerRequest(String email, String password, String role) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        request.setFirstName(" Angel ");
        request.setLastName(" Ruiz ");
        request.setPhone(" 2281234567 ");
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest(String email, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode(code);
        request.setNewPassword(newPassword);
        return request;
    }

    private Account account(String email, boolean active, boolean emailVerified) {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setEmail(email);
        account.setPasswordHash("encoded-password");
        account.setRole(Role.CUSTOMER);
        account.setIsActive(active);
        account.setEmailVerified(emailVerified);
        return account;
    }

    private RefreshToken refreshToken(Account account, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccount(account);
        refreshToken.setToken(token);
        return refreshToken;
    }
}
