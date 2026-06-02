package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorsFilterTest {

    private final GatewayCorsFilter filter = new GatewayCorsFilter();

    @Test
    void shouldAllowSimulatedPaymentTokenHeaderForAllowedOrigin() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/payments/payment-1/approve")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        );

        filter.filter(exchange, exchangeToContinue -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .contains("Authorization")
                .contains("Content-Type")
                .contains("Accept")
                .contains("Origin")
                .contains("X-Simulated-Payment-Token");
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSkipCorsHeadersForWebSocketSockJsInfoEndpoint() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/ws/info")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).isNull();
        assertThat(filterChain.wasCalled()).isTrue();
    }

    @Test
    void shouldSkipCorsHeadersForNativeWebSocketTransport() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/ws/123/session-id/websocket")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.UPGRADE, "websocket")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(filterChain.wasCalled()).isTrue();
    }

    private static class RecordingWebFilterChain implements WebFilterChain {

        private final AtomicBoolean called = new AtomicBoolean();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            called.set(true);
            return Mono.empty();
        }

        boolean wasCalled() {
            return called.get();
        }
    }
}
