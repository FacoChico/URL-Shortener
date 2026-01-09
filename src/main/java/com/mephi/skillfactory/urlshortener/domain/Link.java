package com.mephi.skillfactory.urlshortener.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

@Getter
public class Link {
    private final String code;
    private final String targetUrl;
    private final UUID userId;
    private final Instant createdAt;
    private final long ttlSeconds;
    private final AtomicInteger clickCount = new AtomicInteger(0);
    private final int maxClicks;
    private volatile boolean active = true;

    public Link(String code, String targetUrl, UUID userId, long ttlSeconds, int maxClicks) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.ttlSeconds = ttlSeconds;
        this.maxClicks = maxClicks;
    }

    public int incrementAndGetClicks() {
        return clickCount.incrementAndGet();
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isExpired() {
        return Instant.now()
            .isAfter(createdAt.plusSeconds(ttlSeconds));
    }
}
