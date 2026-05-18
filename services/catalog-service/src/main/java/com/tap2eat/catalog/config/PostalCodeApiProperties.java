package com.tap2eat.catalog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "postal-code.api")
public class PostalCodeApiProperties {

    private String baseUrl;
    private String token;
    private String type;
}