package com.tap2eat.identity.services;

import com.tap2eat.identity.dtos.request.LoginRequest;
import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.response.LoginResponse;
import com.tap2eat.identity.dtos.response.MeResponse;
import com.tap2eat.identity.dtos.response.RegisterResponse;

public interface IAuthService {
    RegisterResponse registerAccount(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    MeResponse getCurrentAccount(String email);
}