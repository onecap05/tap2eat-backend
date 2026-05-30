package com.tap2eat.identity.services;

import com.tap2eat.identity.config.RsaKeyProperties;
import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private Account account;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RsaKeyProperties rsaKeyProperties = new RsaKeyProperties();
        rsaKeyProperties.setPrivateKeyPath(resource(pem("PRIVATE KEY", keyPair.getPrivate().getEncoded())));
        rsaKeyProperties.setPublicKeyPath(resource(pem("PUBLIC KEY", keyPair.getPublic().getEncoded())));

        jwtService = new JwtService(rsaKeyProperties);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 120000L);

        account = new Account();
        account.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setEmail("user@example.com");
        account.setRole(Role.CUSTOMER);
    }

    @Test
    void generateToken_shouldIncludeAccountEmailAsSubject() {
        String token = jwtService.generateToken(account);

        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, account));
    }

    @Test
    void isTokenValid_whenTokenSubjectDoesNotMatchAccount_shouldReturnFalse() {
        String token = jwtService.generateToken(account);
        Account otherAccount = new Account();
        otherAccount.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        otherAccount.setEmail("other@example.com");
        otherAccount.setRole(Role.CUSTOMER);

        assertFalse(jwtService.isTokenValid(token, otherAccount));
    }

    @Test
    void getJwtExpiration_shouldReturnConfiguredExpiration() {
        assertEquals(120000L, jwtService.getJwtExpiration());
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
