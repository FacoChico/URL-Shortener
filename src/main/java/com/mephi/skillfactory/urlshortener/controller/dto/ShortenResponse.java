package com.mephi.skillfactory.urlshortener.controller.dto;

public record ShortenResponse(String code,
                              String shortUrl,
                              String userId) {
}
