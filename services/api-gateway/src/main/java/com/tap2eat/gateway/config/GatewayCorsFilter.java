package com.tap2eat.gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class GatewayCorsFilter implements WebFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
    );

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS =
            "Authorization, Content-Type, Accept, Origin, X-Simulated-Payment-Token";
    private static final String WEBSOCKET_PATH = "/ws";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isWebSocketSockJsRequest(exchange)) {
            return chain.filter(exchange);
        }

        String origin = exchange.getRequest().getHeaders().getOrigin();

        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.set(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
            headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isWebSocketSockJsRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();

        return WEBSOCKET_PATH.equals(path) || path.startsWith(WEBSOCKET_PATH + "/");
    }
}
