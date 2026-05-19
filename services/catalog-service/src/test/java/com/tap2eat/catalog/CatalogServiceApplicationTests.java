package com.tap2eat.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"cloudinary.cloud-name=test-cloud",
		"cloudinary.api-key=test-key",
		"cloudinary.api-secret=test-secret"
})
class CatalogServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
