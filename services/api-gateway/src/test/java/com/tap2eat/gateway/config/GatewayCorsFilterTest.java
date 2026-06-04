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
    void shouldReturnPreflightHeadersWithoutDuplicatesForAllowedOrigin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/orders")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .containsExactly("http://127.0.0.1:4200");
        assertThat(headers.get(HttpHeaders.VARY)).containsExactly(HttpHeaders.ORIGIN);
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .containsExactly("GET, POST, PUT, PATCH, DELETE, OPTIONS");
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).containsExactly("true");
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).containsExactly("3600");
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filterChain.wasCalled()).isFalse();
    }

    @Test
    void shouldCompletePreflightWithoutCorsHeadersForDisallowedOrigin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/orders")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filterChain.wasCalled()).isFalse();
    }

    @Test
    void shouldAddCorsHeadersAndContinueForAllowedNonPreflightRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/customer/restaurants")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:4200");
        assertThat(filterChain.wasCalled()).isTrue();
    }

    @Test
    void shouldContinueWithoutCorsHeadersWhenOriginIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/customer/restaurants")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(filterChain.wasCalled()).isTrue();
    }

    @Test
    void shouldSkipCorsHeadersForWebSocketRootEndpoint() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/ws")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        );
        RecordingWebFilterChain filterChain = new RecordingWebFilterChain();

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(filterChain.wasCalled()).isTrue();
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
