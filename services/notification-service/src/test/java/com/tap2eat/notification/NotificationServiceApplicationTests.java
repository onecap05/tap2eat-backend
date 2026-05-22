package com.tap2eat.notification;

import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"gmail.credentials.path=src/test/resources/google/credentials.json",
		"gmail.tokens.dir=target/test-google-tokens",
		"gmail.from=test@tap2eat.local",
		"grpc.server.port=0",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.listener.direct.auto-startup=false",
		"spring.rabbitmq.dynamic=false"
})
class NotificationServiceApplicationTests {

	@MockitoBean
	private Gmail gmail;

	@Test
	void contextLoads() {
	}

}
