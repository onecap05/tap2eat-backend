package com.tap2eat.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WebSocketConfig.class);

    @Test
    void shouldLoadWebSocketConfig() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(WebSocketMessageBrokerConfigurer.class));
    }
}
