package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteConfigTest {

    private final Properties properties = loadProperties();

    @Test
    void shouldKeepExistingRestRoutesOnHttpDestinations() {
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[0].uri"))
                .isEqualTo("http://identity-service:8081");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[1].uri"))
                .isEqualTo("http://catalog-service:8082");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[7].uri"))
                .isEqualTo("http://order-service:8085");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[13].uri"))
                .isEqualTo("http://finance-service:8083");
    }

    @Test
    void shouldRouteNativeWebSocketTransportWithWsScheme() {
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[14].id"))
                .isEqualTo("notification-websocket-native");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[14].uri"))
                .isEqualTo("ws://notification-service:8084");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[14].predicates[0]"))
                .isEqualTo("Path=/ws,/ws/**");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[14].predicates[1]"))
                .isEqualTo("Header=Upgrade, websocket");
    }

    @Test
    void shouldRouteSockJsHttpHandshakeAndFallbackTransportsWithHttpScheme() {
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[15].id"))
                .isEqualTo("notification-websocket-sockjs");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[15].uri"))
                .isEqualTo("http://notification-service:8084");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[15].predicates[0]"))
                .isEqualTo("Path=/ws,/ws/**");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream stream = GatewayRouteConfigTest.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            properties.load(stream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load application.properties", exception);
        }
    }
}
