package com.tap2eat.identity.services;

import com.tap2eat.identity.dtos.request.*;
import com.tap2eat.identity.dtos.response.*;

public interface IAuthService {
    RegisterResponse registerAccount(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    MeResponse getCurrentAccount(String email);
    void logout(LogoutRequest request);
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    VerifyEmailResponse verifyEmail(VerifyEmailRequest request);
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
    ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request);
}