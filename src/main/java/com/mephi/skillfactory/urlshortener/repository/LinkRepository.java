package com.mephi.skillfactory.urlshortener.repository;

import com.mephi.skillfactory.urlshortener.domain.Link;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {

    Optional<Link> findLinkByCode(String code);

    void saveLink(Link link);

    List<Link> findAll();

    List<Link> findLinksByUserId(UUID userId);

    void deleteLinkByCode(String code);
}
