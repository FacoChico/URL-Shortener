package com.mephi.skillfactory.urlshortener.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    long cleanupIntervalSeconds
) {
}
