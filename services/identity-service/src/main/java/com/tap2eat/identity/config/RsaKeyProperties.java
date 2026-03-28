package com.tap2eat.identity.config;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProperties {

    @Value("classpath:keys/private_key.pem")
    private Resource privateKey;

    @Value("classpath:keys/public_key.pem")
    private Resource publicKey;

    public Resource getPrivateKey() {
        return privateKey;
    }

    public Resource getPublicKey() {
        return publicKey;
    }
}