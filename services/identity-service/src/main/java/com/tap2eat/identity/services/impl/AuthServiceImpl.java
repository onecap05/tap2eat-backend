package com.tap2eat.identity.services.impl;

import com.tap2eat.identity.dtos.request.*;
import com.tap2eat.identity.dtos.response.*;
import com.tap2eat.identity.exceptions.EmailAlreadyRegisteredException;
import com.tap2eat.identity.exceptions.InvalidRoleException;
import com.tap2eat.identity.exceptions.WeakPasswordException;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.RefreshToken;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import com.tap2eat.identity.services.IAuthService;
import com.tap2eat.identity.services.JwtService;
import com.tap2eat.identity.services.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.tap2eat.identity.exceptions.InactiveAccountException;
import com.tap2eat.identity.exceptions.InvalidCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import com.tap2eat.identity.models.EmailVerificationCode;
import com.tap2eat.identity.services.IEmailVerificationCodeService;
import com.tap2eat.identity.services.NotificationGrpcClient;
import com.tap2eat.identity.models.PasswordResetCode;
import com.tap2eat.identity.services.IPasswordResetCodeService;
import com.tap2eat.identity.exceptions.EmailNotVerifiedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import com.tap2eat.identity.models.AccountProfile;

import java.util.Locale;

@Service
public class AuthServiceImpl implements IAuthService {

    private final IAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final IEmailVerificationCodeService emailVerificationCodeService;
    private final NotificationGrpcClient notificationGrpcClient;
    private final IPasswordResetCodeService passwordResetCodeService;
    private final MessageSource messageSource;
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    @Value("${auth.password.min-length}")
    private int passwordMinLength;

    @Autowired
    public AuthServiceImpl(IAccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService, RefreshTokenService refreshTokenService,
                           IEmailVerificationCodeService emailVerificationCodeService,
                           NotificationGrpcClient notificationGrpcClient,
                           IPasswordResetCodeService passwordResetCodeService,
                           MessageSource messageSource) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationCodeService = emailVerificationCodeService;
        this.notificationGrpcClient = notificationGrpcClient;
        this.passwordResetCodeService = passwordResetCodeService;
        this.messageSource = messageSource;
    }

    @Override
    public RegisterResponse registerAccount(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(getMessage("auth.email.already.registered"));
        }

        validatePasswordStrength(request.getPassword());
        Role validatedRole = validateRole(request.getRole());

        Account newAccount = new Account();
        newAccount.setEmail(normalizedEmail);
        newAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newAccount.setRole(validatedRole);
        newAccount.setEmailVerified(false);
        newAccount.setIsActive(true);

        AccountProfile accountProfile = new AccountProfile();
        accountProfile.setAccount(newAccount);
        accountProfile.setFirstName(request.getFirstName().trim());
        accountProfile.setLastName(request.getLastName().trim());
        accountProfile.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty()
                ? request.getPhone().trim()
                : null);

        newAccount.setProfile(accountProfile);

        Account savedAccount = accountRepository.save(newAccount);
        EmailVerificationCode verificationCode = emailVerificationCodeService.createCode(savedAccount);
        notificationGrpcClient.sendVerificationEmail(savedAccount.getEmail(), verificationCode.getCode());

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                savedAccount.getRole().name(),
                getMessage("auth.account.created.verify")
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(getMessage("auth.invalid.credentials")));

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new InactiveAccountException(getMessage("auth.account.inactive"));
        }

        if (!Boolean.TRUE.equals(account.getEmailVerified())) {
            throw new EmailNotVerifiedException(getMessage("auth.email.not.verified"));
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), account.getPasswordHash());

        if (!passwordMatches) {
            throw new InvalidCredentialsException(getMessage("auth.invalid.credentials"));
        }

        String accessToken = jwtService.generateToken(account);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                TOKEN_TYPE_BEARER,
                jwtService.getJwtExpiration()
        );
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    @Transactional(readOnly = true)
    @Override
    public MeResponse getCurrentAccount(String email) {
        String normalizedEmail = normalizeEmail(email);

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(getMessage("auth.authenticated.account.not.found")));

        AccountProfile profile = account.getProfile();

        return new MeResponse(
                account.getId(),
                account.getEmail(),
                account.getRole().name(),
                account.getIsActive(),
                account.getEmailVerified(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getPhone() : null
        );
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(normalizedEmail).orElse(null);

        if (account == null) {
            return new ForgotPasswordResponse(getMessage("auth.forgot.password.sent"));
        }

        PasswordResetCode passwordResetCode = passwordResetCodeService.createCode(account);

        notificationGrpcClient.sendVerificationEmail(
                account.getEmail(),
                passwordResetCode.getCode()
        );

        return new ForgotPasswordResponse(getMessage("auth.forgot.password.sent"));
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(getMessage("auth.reset.request.invalid")));

        PasswordResetCode passwordResetCode = passwordResetCodeService.validateCode(request.getCode());

        if (!passwordResetCode.getAccount().getId().equals(account.getId())) {
            throw new InvalidCredentialsException(getMessage("auth.reset.request.invalid"));
        }

        validatePasswordStrength(request.getNewPassword());

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        passwordResetCodeService.markAsUsed(passwordResetCode);
        refreshTokenService.deleteByAccountId(account.getId());

        return new ResetPasswordResponse(getMessage("auth.password.reset.success"));
    }

    @Override
    @Transactional
    public ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(normalizedEmail).orElse(null);

        if (account == null) {
            return new ResendVerificationCodeResponse(getMessage("auth.verification.code.resent"));
        }

        if (Boolean.TRUE.equals(account.getEmailVerified())) {
            return new ResendVerificationCodeResponse(getMessage("auth.email.already.verified"));
        }

        EmailVerificationCode verificationCode = emailVerificationCodeService.createCode(account);

        notificationGrpcClient.sendVerificationEmail(
                account.getEmail(),
                verificationCode.getCode()
        );

        return new ResendVerificationCodeResponse(getMessage("auth.verification.code.resent"));
    }

    @Transactional(readOnly = true)
    @Override
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        Account account = refreshToken.getAccount();
        String newAccessToken = jwtService.generateToken(account);

        return new TokenRefreshResponse(
                newAccessToken,
                refreshToken.getToken(),
                TOKEN_TYPE_BEARER
        );
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationCode verificationCode = emailVerificationCodeService.validateCode(request.getCode());

        Account account = verificationCode.getAccount();
        account.setEmailVerified(true);
        accountRepository.save(account);

        emailVerificationCodeService.markAsUsed(verificationCode);

        return new VerifyEmailResponse(getMessage("auth.email.verified.success"));
    }

    private Role validateRole(String roleValue){
        if (roleValue == null || roleValue.trim().isEmpty()){
            throw new InvalidRoleException(getMessage("auth.role.required"));
        }

        Role role;
        try{
            role = Role.valueOf(roleValue.trim().toUpperCase());

        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException(getMessage("auth.role.invalid", roleValue));
        } catch (Exception ex) {
            throw new InvalidRoleException(getMessage("auth.role.validation.error", roleValue));
        }

        if (role == Role.ADMIN) {
            throw new InvalidRoleException(getMessage("auth.role.admin.not.allowed"));
        }

        return role;
    }

    //TODO arreglar para poner que sea  8 en una proxima iteración, se dejo asi por terminos de registro para cuentas de desarrollo
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < passwordMinLength) {
            throw new WeakPasswordException(getMessage("auth.password.min.length", passwordMinLength));
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        if (!hasUpper) {
            throw new WeakPasswordException(getMessage("auth.password.uppercase.required"));
        }

        boolean hasLower = password.matches(".*[a-z].*");
        if (!hasLower) {
            throw new WeakPasswordException(getMessage("auth.password.lowercase.required"));
        }

        boolean hasDigit = password.matches(".*\\d.*");
        if (!hasDigit) {
            throw new WeakPasswordException(getMessage("auth.password.digit.required"));
        }

        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        if (!hasSpecial) {
            throw new WeakPasswordException(getMessage("auth.password.special.required"));
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}