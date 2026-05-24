package com.tap2eat.gateway.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GatewayCorsFilterTest {

    private final GatewayCorsFilter filter = new GatewayCorsFilter();

    @Test
    void shouldAllowSimulatedPaymentTokenHeaderForAllowedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/payments/payment-1/approve");
        request.addHeader("Origin", "http://localhost:4200");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .contains("Authorization")
                .contains("Content-Type")
                .contains("Accept")
                .contains("Origin")
                .contains("X-Simulated-Payment-Token");
    }

    @Test
    void shouldAllowCorsPreflightForWebSocketSockJsInfoEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/ws/info");
        request.addHeader("Origin", "http://localhost:4200");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:4200");
        assertThat(response.getHeader("Access-Control-Allow-Methods"))
                .contains("GET")
                .contains("POST")
                .contains("OPTIONS");
        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .contains("Authorization")
                .contains("Content-Type")
                .contains("Accept")
                .contains("Origin")
                .contains("X-Simulated-Payment-Token");
    }
}
