package com.tap2eat.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner printMongoConfig(Environment env) {
		return args -> {
			System.out.println("MONGO URI -> " + env.getProperty("spring.data.mongodb.uri"));
			System.out.println("MONGO HOST -> " + env.getProperty("spring.data.mongodb.host"));
			System.out.println("MONGO PORT -> " + env.getProperty("spring.data.mongodb.port"));
			System.out.println("MONGO DATABASE -> " + env.getProperty("spring.data.mongodb.database"));
			System.out.println("MONGO USERNAME -> " + env.getProperty("spring.data.mongodb.username"));
		};
	}
}