package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthorityConverterConfigTest {

    private final Converter<Jwt, Collection<GrantedAuthority>> converter =
            new JwtAuthorityConverterConfig().jwtGrantedAuthoritiesConverter();

    @Test
    void shouldConvertRoleClaimToPrefixedAuthority() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt("RESTAURANT_OWNER", List.of()));

        assertThat(authorityNames(authorities)).contains("ROLE_RESTAURANT_OWNER");
    }

    @Test
    void shouldKeepAlreadyPrefixedRoleClaim() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt("ROLE_RESTAURANT_OWNER", List.of()));

        assertThat(authorityNames(authorities)).contains("ROLE_RESTAURANT_OWNER");
    }

    @Test
    void shouldIgnoreBlankRoleClaim() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(" ", List.of()));

        assertThat(authorityNames(authorities)).doesNotContain("ROLE_ ");
    }

    @Test
    void shouldPreserveDefaultScopeAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(null, List.of("orders:read")));

        assertThat(authorityNames(authorities)).contains("SCOPE_orders:read");
    }

    private static Jwt jwt(String role, List<String> scopes) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-05-22T10:15:30Z"))
                .expiresAt(Instant.parse("2026-05-22T11:15:30Z"))
                .claim("scope", String.join(" ", scopes));

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.build();
    }

    private static List<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
