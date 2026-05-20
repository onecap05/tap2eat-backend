package com.tap2eat.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CatalogClockConfig {

    @Bean
    public Clock catalogClock() {
        return Clock.systemDefaultZone();
    }
}
