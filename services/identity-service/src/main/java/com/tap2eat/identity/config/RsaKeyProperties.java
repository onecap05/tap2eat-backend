package com.tap2eat.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class RsaKeyProperties {

    private Resource privateKeyPath;
    private Resource publicKeyPath;

    public Resource getPrivateKey() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(Resource privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public Resource getPublicKey() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(Resource publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
}