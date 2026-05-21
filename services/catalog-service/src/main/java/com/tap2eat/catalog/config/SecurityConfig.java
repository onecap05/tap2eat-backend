package com.tap2eat.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Configuration
public class SecurityConfig {

    private static final String RESTAURANT_OWNER_ROLE = "RESTAURANT_OWNER";

    private static final String[] PUBLIC_SYSTEM_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] PUBLIC_CUSTOMER_ENDPOINTS = {
            "/api/customer/**"
    };

    private static final String[] INTERNAL_ENDPOINTS = {
            "/internal/catalog/**"
    };

    private static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter,
            @Value("${tap2eat.internal.service-token:tap2eat-internal-dev-token}") String internalServiceToken
    ) throws Exception {
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_SYSTEM_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_CUSTOMER_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, INTERNAL_ENDPOINTS)
                        .access((authentication, context) -> new AuthorizationDecision(
                                internalServiceToken.equals(context.getRequest().getHeader(INTERNAL_SERVICE_TOKEN_HEADER))
                        ))
                        .anyRequest().hasRole(RESTAURANT_OWNER_ROLE)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                );

        return http.build();
    }
}
