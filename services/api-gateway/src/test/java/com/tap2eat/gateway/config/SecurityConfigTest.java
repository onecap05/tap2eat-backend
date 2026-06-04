package com.tap2eat.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Field;
import java.util.Arrays;

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

    @Test
    void shouldDeclarePublicAuthRoutes() throws Exception {
        String[] publicAuthEndpoints = readStringArray("PUBLIC_AUTH_ENDPOINTS");

        assertThat(publicAuthEndpoints)
                .containsExactly(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/refresh",
                        "/api/auth/verify-email",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/resend-verification-code"
                );
    }

    @Test
    void shouldDeclarePublicHealthActuatorRoutes() throws Exception {
        String[] publicSystemEndpoints = readStringArray("PUBLIC_SYSTEM_ENDPOINTS");

        assertThat(publicSystemEndpoints).containsExactly("/actuator/health", "/actuator/info");
    }

    @Test
    void shouldDeclareOwnerCatalogRoutesAsProtected() throws Exception {
        String[] ownerCatalogEndpoints = readStringArray("OWNER_CATALOG_ENDPOINTS");

        assertThat(ownerCatalogEndpoints)
                .contains(
                        "/api/restaurants/**",
                        "/api/branches/**",
                        "/api/categories/**",
                        "/api/products/**",
                        "/api/uploads/**",
                        "/api/locations/**"
                );
        assertThat(Arrays.asList(ownerCatalogEndpoints)).doesNotContain("/api/orders/**", "/api/payments/**");
    }

    @Test
    void shouldDeclareCustomerCatalogGetRoutesAsPublic() throws Exception {
        String[] publicCustomerEndpoints = readStringArray("PUBLIC_CUSTOMER_ENDPOINTS");

        assertThat(publicCustomerEndpoints).containsExactly("/api/customer/**");
    }

    private static String[] readStringArray(String fieldName) throws Exception {
        Field field = SecurityConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
