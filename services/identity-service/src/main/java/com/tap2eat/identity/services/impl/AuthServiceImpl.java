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

@Service
public class AuthServiceImpl implements IAuthService {

    private final IAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final IEmailVerificationCodeService emailVerificationCodeService;

    @Autowired
    public AuthServiceImpl(IAccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService, RefreshTokenService refreshTokenService,
                           IEmailVerificationCodeService emailVerificationCodeService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationCodeService = emailVerificationCodeService;
    }

    @Override
    public RegisterResponse registerAccount(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException("The email is already registered.");
        }

        validatePasswordStrength(request.getPassword());
        Role validatedRole = validateRole(request.getRole());

        Account newAccount = new Account();
        newAccount.setEmail(normalizedEmail);
        newAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newAccount.setRole(validatedRole);
        newAccount.setEmailVerified(false);
        newAccount.setIsActive(true);

        Account savedAccount = accountRepository.save(newAccount);
        EmailVerificationCode verificationCode = emailVerificationCodeService.createCode(savedAccount);

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                savedAccount.getRole().name(),
                "Account created successfully. Please verify your email."
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new InactiveAccountException("The account is inactive.");
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), account.getPasswordHash());

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        String accessToken = jwtService.generateToken(account);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getJwtExpiration()
        );
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    @Override
    public MeResponse getCurrentAccount(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated account not found."));

        return new MeResponse(
                account.getId(),
                account.getEmail(),
                account.getRole().name(),
                account.getIsActive()
        );
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
                "Bearer"
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

        return new VerifyEmailResponse("Email verified successfully.");
    }

    private Role validateRole(String roleValue){
        if (roleValue == null || roleValue.trim().isEmpty()){
            throw new InvalidRoleException("Role is required.");
        }

        Role role;
        try{
            role = Role.valueOf(roleValue.trim().toUpperCase());

        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException("Invalid role: " + roleValue);
        } catch (Exception ex){
            throw new InvalidRoleException("An error occurred while validating the role: " + roleValue);
        }

        if (role == Role.ADMIN){
            throw new InvalidRoleException("Admin role cannot be assigned through public registration.");
        }
        return role;
    }

    //TODO arreglar para poner que sea  8 en una proxima iteración, se dejo asi por terminos de registro para cuentas de desarrollo
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 1) {
            throw new WeakPasswordException("Password must be at least 8 characters long.");
        }

        boolean hasUpper = password.matches(".*[A-Z].*");

        if (!hasUpper) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter.");
        }

        boolean hasLower = password.matches(".*[a-z].*");

        if (!hasLower){
            throw new WeakPasswordException("Password must contain at least one lowercase letter.");
        }

        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasDigit){
            throw new WeakPasswordException("Password must contain at least one digit.");
        }

        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

        if (!hasSpecial){
            throw new WeakPasswordException("Password must contain at least one special character.");
        }
    }
}