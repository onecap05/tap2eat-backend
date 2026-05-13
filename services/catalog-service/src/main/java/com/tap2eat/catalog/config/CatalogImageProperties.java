package com.tap2eat.catalog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "catalog.images")
public class CatalogImageProperties {

    private String defaultProductUrl;
    private String defaultProductObjectKey;
}