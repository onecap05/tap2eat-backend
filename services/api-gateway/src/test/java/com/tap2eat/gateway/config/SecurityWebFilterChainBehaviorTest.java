package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SecurityWebFilterChainBehaviorTest.TestApplication.class
)
class SecurityWebFilterChainBehaviorTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void optionsPreflight_shouldBePermittedForProtectedRoute() {
        webTestClient.method(HttpMethod.OPTIONS)
                .uri("/api/products/product-1")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
    }

    @Test
    void authRoute_shouldBePermittedWithoutToken() {
        webTestClient.post()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void healthRoute_shouldBePermittedWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void websocketRoute_shouldBePermittedWithoutToken() {
        webTestClient.get()
                .uri("/ws/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ownerCatalogRoute_shouldRejectMissingTokenAndRequireOwnerRole() {
        webTestClient.get()
                .uri("/api/products/product-1")
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri("/api/products/product-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer customer-token")
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.get()
                .uri("/api/products/product-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer owner-token")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedServiceRoutes_shouldRequireAuthenticatedRequest() {
        assertRequiresAuthentication("/api/orders/order-1");
        assertRequiresAuthentication("/api/payments/payment-1");
        assertRequiresAuthentication("/api/reports/sales");
    }

    private void assertRequiresAuthentication(String path) {
        webTestClient.get()
                .uri(path)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer customer-token")
                .exchange()
                .expectStatus().isOk();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.cloud.gateway.config.GatewayAutoConfiguration",
            "org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration"
    })
    @Import({SecurityConfig.class, JwtAuthorityConverterConfig.class, GatewayCorsFilter.class})
    static class TestApplication {

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(jwt(token));
        }

        @Bean
        RouterFunction<ServerResponse> testRoutes() {
            return RouterFunctions.route(RequestPredicates.all(), request -> ServerResponse.ok().build());
        }

        private static Jwt jwt(String token) {
            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.parse("2026-05-22T10:15:30Z"))
                    .expiresAt(Instant.parse("2026-05-22T11:15:30Z"));

            if ("owner-token".equals(token)) {
                builder.claim("role", "RESTAURANT_OWNER");
            } else {
                builder.claim("role", "CUSTOMER");
            }

            return builder.build();
        }
    }
}
