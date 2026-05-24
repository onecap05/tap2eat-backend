package com.tap2eat.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.Collection;

@Configuration
public class SecurityConfig {

    private static final String RESTAURANT_OWNER_ROLE = "RESTAURANT_OWNER";

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/auth/verify-email",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/resend-verification-code"
    };

    private static final String[] PUBLIC_SYSTEM_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info"
    };

    private static final String[] PUBLIC_CUSTOMER_ENDPOINTS = {
            "/api/customer/**"
    };

    private static final String[] OWNER_CATALOG_ENDPOINTS = {
            "/api/restaurants/**",
            "/api/branches/**",
            "/api/categories/**",
            "/api/products/**",
            "/api/uploads/**",
            "/api/locations/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter
    ) throws Exception {
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_SYSTEM_ENDPOINTS).permitAll()
                        // SockJS cannot reliably send Authorization headers during every handshake transport.
                        // Keep only the WebSocket endpoint public temporarily; REST APIs remain protected.
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_CUSTOMER_ENDPOINTS).permitAll()
                        .requestMatchers(OWNER_CATALOG_ENDPOINTS).hasRole(RESTAURANT_OWNER_ROLE)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                );

        return http.build();
    }
}
