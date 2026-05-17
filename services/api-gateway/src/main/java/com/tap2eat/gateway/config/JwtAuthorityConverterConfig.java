package com.tap2eat.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class JwtAuthorityConverterConfig {

    private static final String ROLE_CLAIM = "role";
    private static final String ROLE_PREFIX = "ROLE_";

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
            if (defaultAuthorities != null) {
                authorities.addAll(defaultAuthorities);
            }

            String role = jwt.getClaimAsString(ROLE_CLAIM);
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(normalizeRole(role)));
            }

            return authorities;
        };
    }

    private String normalizeRole(String role) {
        return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
    }
}