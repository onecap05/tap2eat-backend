package com.tap2eat.identity.config;

import com.tap2eat.identity.models.Account;
import com.tap2eat.identity.models.Role;
import com.tap2eat.identity.repositories.IAccountRepository;
import com.tap2eat.identity.services.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private IAccountRepository accountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenAuthorizationHeaderIsMissing_shouldContinueWithoutAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_whenAuthorizationHeaderDoesNotUseBearer_shouldContinueWithoutAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        request.addHeader("Authorization", "Basic token");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_whenTokenIsValidAndAccountIsActive_shouldSetAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        Account account = account(true);
        request.addHeader("Authorization", "Bearer jwt-token");

        when(jwtService.extractUsername("jwt-token")).thenReturn("user@example.com");
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(jwtService.isTokenValid("jwt-token", account)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
    }

    @Test
    void doFilter_whenAccountIsInactive_shouldNotSetAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        Account account = account(false);
        request.addHeader("Authorization", "Bearer jwt-token");

        when(jwtService.extractUsername("jwt-token")).thenReturn("user@example.com");
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).isTokenValid("jwt-token", account);
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExists_shouldKeepExistingAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        UsernamePasswordAuthenticationToken existingAuthentication =
                new UsernamePasswordAuthenticationToken("existing@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);
        request.addHeader("Authorization", "Bearer jwt-token");

        when(jwtService.extractUsername("jwt-token")).thenReturn("user@example.com");

        filter.doFilter(request, response, filterChain);

        assertSame(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());
        verify(accountRepository, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilter_whenJwtParsingFails_shouldClearAuthenticationAndContinue()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, accountRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("existing@example.com", null));
        request.addHeader("Authorization", "Bearer broken-token");

        when(jwtService.extractUsername("broken-token")).thenThrow(new IllegalArgumentException("bad token"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, filterChain.getRequest());
    }

    private Account account(boolean active) {
        Account account = new Account();
        account.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setEmail("user@example.com");
        account.setRole(Role.CUSTOMER);
        account.setIsActive(active);
        account.setEmailVerified(true);
        return account;
    }
}
