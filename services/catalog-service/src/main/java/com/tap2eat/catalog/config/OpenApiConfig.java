package com.tap2eat.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tap2Eat Catalog Service API")
                        .version("1.0.0")
                        .description("API documentation for Tap2Eat catalog management: restaurants, branches, categories, products and image uploads.")
                        .contact(new Contact()
                                .name("Tap2Eat Team")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Catalog Service Direct URL"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway URL")
                ));
    }
}