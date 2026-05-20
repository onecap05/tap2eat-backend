package com.tap2eat.catalog.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class MongoIntegrationTestBase {

    protected static final String TEST_DATABASE_NAME = "tap2eat_catalog_test";

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> TEST_DATABASE_NAME);
    }

    @BeforeEach
    void cleanBeforeEach() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfterEach() {
        cleanDatabase();
    }

    protected void cleanDatabase() {
        dropCollection("restaurants");
        dropCollection("branches");
        dropCollection("categories");
        dropCollection("products");
        dropCollection("branch_product_overrides");
        dropCollection("branch_modifier_option_overrides");
        dropCollection("postal_codes");
    }

    private void dropCollection(String collectionName) {
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }
    }
}
