package com.tap2eat.identity.services;

import com.tap2eat.identity.dtos.request.RegisterRequest;
import com.tap2eat.identity.dtos.response.RegisterResponse;

public interface IAuthService {
    RegisterResponse registerAccount(RegisterRequest request);
}