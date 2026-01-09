package com.mephi.skillfactory.urlshortener.controller;

import com.mephi.skillfactory.urlshortener.controller.dto.ShortenRequest;
import com.mephi.skillfactory.urlshortener.controller.dto.ShortenResponse;
import com.mephi.skillfactory.urlshortener.domain.Link;
import com.mephi.skillfactory.urlshortener.service.NotificationService;
import com.mephi.skillfactory.urlshortener.service.ShortLinkService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ShortLinkController {
    private static final String USER_ID_HEADER = "X-User-Id";
    private final ShortLinkService shortLinkService;
    private final NotificationService notificationService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody ShortenRequest request,
                                                   @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        final var shortLink = shortLinkService.createShortLink(request.url(), userId, request.maxClicks(), request.ttlSeconds());
        final var shortLinkUrl = shortLinkService.constructShortLinkUrl(shortLink);
        final var responseBody = new ShortenResponse(shortLink.getCode(), shortLinkUrl, shortLink.getUserId().toString());
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> redirect(@PathVariable String code) {
        final var shortLinkOptional = shortLinkService.getShortLink(code);
        if (shortLinkOptional.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Link not found or inactive/expired");
        }

        final var shortLink = shortLinkOptional.get();
        final var uri = URI.create(shortLink.getTargetUrl());
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(uri)
            .build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<String>> notifications(@RequestHeader(USER_ID_HEADER) UUID user) {
        return ResponseEntity.ok(notificationService.getNotifications(user));
    }

    @DeleteMapping("/links/{code}")
    public ResponseEntity<?> delete(@PathVariable String code, @RequestHeader(USER_ID_HEADER) UUID user) {
        final var isDeleted = shortLinkService.deleteLink(code, user);
        if (!isDeleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/links")
    public ResponseEntity<List<Link>> list(@RequestHeader(USER_ID_HEADER) UUID user) {
        return ResponseEntity.ok(shortLinkService.listByUserId(user));
    }
}
