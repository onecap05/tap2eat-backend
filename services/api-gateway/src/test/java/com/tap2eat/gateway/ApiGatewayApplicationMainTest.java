package com.tap2eat.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ApiGatewayApplicationMainTest {

    @Test
    void main_shouldDelegateToSpringApplication() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ApiGatewayApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ApiGatewayApplication.class, args));
        }
    }
}
