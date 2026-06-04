package com.tap2eat.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigUtilitiesTest {

    @Test
    void pemUtils_shouldReadPrivateAndPublicKeysFromPemResources() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        PrivateKey privateKey = PemUtils.readPrivateKey(resource(pem("PRIVATE KEY", keyPair.getPrivate().getEncoded())));
        PublicKey publicKey = PemUtils.readPublicKey(resource(pem("PUBLIC KEY", keyPair.getPublic().getEncoded())));

        assertInstanceOf(PrivateKey.class, privateKey);
        assertInstanceOf(PublicKey.class, publicKey);
    }

    @Test
    void rsaKeyProperties_shouldReturnConfiguredResources() {
        ByteArrayResource privateKeyResource = resource("private");
        ByteArrayResource publicKeyResource = resource("public");
        RsaKeyProperties properties = new RsaKeyProperties();

        properties.setPrivateKeyPath(privateKeyResource);
        properties.setPublicKeyPath(publicKeyResource);

        assertSame(privateKeyResource, properties.getPrivateKey());
        assertSame(publicKeyResource, properties.getPublicKey());
    }

    @Test
    void securityConfig_passwordEncoder_shouldReturnBcryptEncoder() {
        SecurityConfig securityConfig = new SecurityConfig(null);

        assertInstanceOf(BCryptPasswordEncoder.class, securityConfig.passwordEncoder());
    }

    @Test
    void openApiConfig_shouldBeInstantiable() {
        assertInstanceOf(OpenApiConfig.class, new OpenApiConfig());
    }

    private ByteArrayResource resource(String value) {
        return new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8));
    }

    private String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}
