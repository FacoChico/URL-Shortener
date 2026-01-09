package com.mephi.skillfactory.urlshortener.controller.dto;

public record ShortenRequest(
    String url,
    Integer maxClicks,
    Long ttlSeconds
) {
}
