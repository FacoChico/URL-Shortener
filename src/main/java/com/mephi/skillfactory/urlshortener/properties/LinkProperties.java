package com.mephi.skillfactory.urlshortener.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "link")
public record LinkProperties(
    String baseUrl,
    long defaultTtlSeconds,
    int defaultMaxClicks,
    int codeLength,
    int maxShortGenAttempts
) {
}
