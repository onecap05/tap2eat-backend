package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void shouldDeclarePublicOrderTrackingRoute() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("PUBLIC_ORDER_ENDPOINTS");
        field.setAccessible(true);

        String[] publicOrderEndpoints = (String[]) field.get(null);

        assertThat(publicOrderEndpoints).contains("/api/orders/public/track/**");
        assertThat(HttpMethod.GET.name()).isEqualTo("GET");
    }
}
