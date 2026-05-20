package com.tap2eat.catalog.security;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedAccountProviderTest {

    private final AuthenticatedAccountProvider provider = new AuthenticatedAccountProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRequiredAccountId_shouldReturnAccountIdClaim() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("owner-1")));

        assertThat(provider.getRequiredAccountId()).isEqualTo("owner-1");
    }

    @Test
    void getRequiredAccountId_shouldRejectMissingAuthenticationOrClaim() {
        assertThatThrownBy(provider::getRequiredAccountId).isInstanceOf(CatalogValidationException.class);

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(" ")));

        assertThatThrownBy(provider::getRequiredAccountId).isInstanceOf(CatalogValidationException.class);
    }

    private Jwt jwt(String accountId) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("accountId", accountId)
        );
    }
}
