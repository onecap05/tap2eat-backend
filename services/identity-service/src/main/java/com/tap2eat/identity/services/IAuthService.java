package com.tap2eat.identity.services;

import com.tap2eat.identity.dtos.request.RegisterRequest;

public interface IAuthService {
    void registerAccount(RegisterRequest request);
}