package com.mephi.skillfactory.urlshortener.service;

import com.mephi.skillfactory.urlshortener.domain.Link;
import com.mephi.skillfactory.urlshortener.properties.AppProperties;
import com.mephi.skillfactory.urlshortener.properties.LinkProperties;
import com.mephi.skillfactory.urlshortener.repository.LinkRepository;
import com.mephi.skillfactory.urlshortener.service.exception.UniqueCodeException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShortLinkService {
    private final ScheduledExecutorService linksCleaner = Executors.newSingleThreadScheduledExecutor();
    private final LinkRepository linkRepository;
    private final CodeGenerator codeGenerator;
    private final AppProperties appProperties;
    private final LinkProperties linkProperties;
    private final NotificationService notificationService;

    public ShortLinkService(LinkRepository linkRepository, CodeGenerator codeGenerator,
                            AppProperties appProperties, LinkProperties linkProperties,
                            NotificationService notificationService) {
        this.linkRepository = linkRepository;
        this.codeGenerator = codeGenerator;
        this.appProperties = appProperties;
        this.linkProperties = linkProperties;
        this.notificationService = notificationService;

        startCleaner();
    }

    public Link createShortLink(String longLink, UUID userId, Integer maxClicks, Long ttlSeconds) {
        if (userId == null) {
            userId = UUID.randomUUID();
            log.debug("New user id is generated: {}", userId);
        }

        for (var attempts = 0; attempts < linkProperties.maxShortGenAttempts(); attempts++) {
            final var code = codeGenerator.generate(userId.toString(), longLink, linkProperties.codeLength(), attempts);
            if (isCodeAvailable(code)) {
                final var ttl = ttlSeconds == null
                    ? linkProperties.defaultTtlSeconds()
                    : ttlSeconds;
                final var clicks = maxClicks == null
                    ? linkProperties.defaultMaxClicks()
                    : maxClicks;

                final var link = new Link(code, longLink, userId, ttl, clicks);
                linkRepository.saveLink(link);
                notificationService.notify(userId, "Link created: " + code);
                return link;
            }
        }

        throw new UniqueCodeException("Cannot generate unique link code after %s attempts"
            .formatted(linkProperties.maxShortGenAttempts()));
    }

    public String constructShortLinkUrl(Link shortLink) {
        return linkProperties.baseUrl() + '/' + shortLink.getCode();
    }

    private boolean isCodeAvailable(String code) {
        return linkRepository.findLinkByCode(code).isEmpty();
    }

    public Optional<Link> getShortLink(String code) {
        final var linkOptional = linkRepository.findLinkByCode(code);
        if (linkOptional.isEmpty()) {
            return Optional.empty();
        }

        final var link = linkOptional.get();
        if (!link.isActive() || link.isExpired()) {
            return Optional.empty();
        }

        final var clicks = link.incrementAndGetClicks();
        if (clicks >= link.getMaxClicks()) {
            link.deactivate();
            notificationService.notify(link.getUserId(), "Link " + code + " reached max clicks and is now inactive");
        }
        return Optional.of(link);
    }

    public List<Link> listByUserId(UUID userId) {
        return linkRepository.findLinksByUserId(userId);
    }

    public boolean deleteLink(String code, UUID userId) {
        final var linkOptional = linkRepository.findLinkByCode(code);
        if (linkOptional.isEmpty()) {
            return false;
        }
        final var link = linkOptional.get();
        if (!link.getUserId().equals(userId)) {
            throw new SecurityException("Only owner can delete link");
        }
        linkRepository.deleteLinkByCode(code);
        notificationService.notify(userId, "Link deleted: " + code);
        return true;
    }

    public void cleanup() {
        for (final var link : linkRepository.findAll()) {
            if (link.isExpired()) {
                linkRepository.deleteLinkByCode(link.getCode());
                notificationService.notify(link.getUserId(), "Link " + link.getCode() + " expired and removed");
            }
        }
    }

    private void startCleaner() {
        linksCleaner.scheduleAtFixedRate(
            this::cleanup, appProperties.cleanupIntervalSeconds(), appProperties.cleanupIntervalSeconds(), TimeUnit.SECONDS
        );
    }
}
