package com.mephi.skillfactory.urlshortener.repository;

import com.mephi.skillfactory.urlshortener.domain.Link;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryLinkRepository implements LinkRepository {
    private final ConcurrentHashMap<String, Link> linkByCode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> codesByUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<Link> findLinkByCode(String code) {
        return Optional.ofNullable(linkByCode.get(code));
    }

    @Override
    public void saveLink(Link link) {
        linkByCode.put(link.getCode(), link);
        codesByUserId.compute(link.getUserId(), (k, v) -> {
            if (v == null) {
                v = ConcurrentHashMap.newKeySet();
            }
            v.add(link.getCode());
            return v;
        });
    }

    @Override
    public List<Link> findAll() {
        return new ArrayList<>(linkByCode.values());
    }

    @Override
    public List<Link> findLinksByUserId(UUID userId) {
        final var codes = codesByUserId.getOrDefault(userId, Collections.emptySet());
        final var out = new ArrayList<Link>();
        for (String code : codes) {
            final var link = linkByCode.get(code);
            if (link != null) {
                out.add(link);
            }
        }
        return out;
    }

    @Override
    public void deleteLinkByCode(String code) {
        final var removed = linkByCode.remove(code);
        if (removed != null) {
            final var codes = codesByUserId.get(removed.getUserId());
            if (codes != null) {
                codes.remove(code);
            }
        }
    }
}
