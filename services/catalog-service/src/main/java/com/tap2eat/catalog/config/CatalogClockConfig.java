package com.tap2eat.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class CatalogClockConfig {

    @Bean
    public Clock catalogClock(
            @Value("${tap2eat.catalog.time-zone:America/Mexico_City}") String timeZone
    ) {
        return Clock.system(ZoneId.of(timeZone));
    }
}