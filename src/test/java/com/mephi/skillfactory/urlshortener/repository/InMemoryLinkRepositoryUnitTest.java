package com.mephi.skillfactory.urlshortener.repository;

import com.mephi.skillfactory.urlshortener.domain.Link;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryLinkRepositoryUnitTest {
    private InMemoryLinkRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLinkRepository();
    }

    @Test
    void shouldReturnSavedLink() {
        final var userId = UUID.randomUUID();
        final var link = mockLink("abc", userId);

        repository.saveLink(link);

        final var linkOptional = repository.findLinkByCode("abc");
        assertTrue(linkOptional.isPresent());
        assertSame(link, linkOptional.get());
    }

    @Test
    void shouldReturnAllSavedLinks() {
        final var link1 = mockLink("c1", UUID.randomUUID());
        final var link2 = mockLink("c2", UUID.randomUUID());

        repository.saveLink(link1);
        repository.saveLink(link2);

        final var links = repository.findAll();
        assertEquals(2, links.size());
        assertTrue(links.contains(link1) && links.contains(link2));
    }

    @Test
    void shouldReturnOnlyUsersLinks() {
        // given
        final var userId1 = UUID.randomUUID();
        final var link11 = mockLink("link11", userId1);
        final var link12 = mockLink("link12", userId1);

        final var userId2 = UUID.randomUUID();
        final var link21 = mockLink("link21", userId2);

        repository.saveLink(link11);
        repository.saveLink(link12);
        repository.saveLink(link21);

        // when
        final var user1Links = repository.findLinksByUserId(userId1);
        final var user2Links = repository.findLinksByUserId(userId2);

        // then
        assertEquals(2, user1Links.size());
        assertTrue(user1Links.contains(link11));
        assertTrue(user1Links.contains(link12));

        assertEquals(1, user2Links.size());
        assertTrue(user2Links.contains(link21));
    }

    @Test
    void shouldDeleteLinkByCode() {
        // given
        final var userId = UUID.randomUUID();
        final var link = mockLink("toDelete", userId);

        repository.saveLink(link);
        assertTrue(repository.findLinkByCode("toDelete").isPresent());
        assertFalse(repository.findLinksByUserId(userId).isEmpty());

        // when
        repository.deleteLinkByCode("toDelete");

        // then
        assertFalse(repository.findLinkByCode("toDelete").isPresent());
        assertTrue(repository.findLinksByUserId(userId).isEmpty());
    }

    @Test
    void shouldNotThrowWhenDeleteNonExistingCodes() {
        final var userId = UUID.randomUUID();
        final var linkToKeep = mockLink("keep", userId);
        repository.saveLink(linkToKeep);

        assertDoesNotThrow(() -> repository.deleteLinkByCode("no-such-code"));

        assertTrue(repository.findLinkByCode("keep").isPresent());
        assertEquals(1, repository.findLinksByUserId(userId).size());
    }

    @Test
    void shouldFindMultipleLinksForSameUser() {
        final var user = UUID.randomUUID();
        final var link1 = mockLink("link1", user);
        final var link2 = mockLink("link2", user);
        repository.saveLink(link1);
        repository.saveLink(link2);

        final var links = repository.findLinksByUserId(user);

        assertEquals(2, links.size());
        final var codes = new HashSet<>();
        for (final var l : links) {
            codes.add(l.getCode());
        }
        assertTrue(codes.containsAll(Arrays.asList("link1", "link2")));
    }

    @Test
    @SneakyThrows
    void shouldSaveAllLinkInConcurrentManner() throws Exception {
        final var threads = 8;
        final var perThread = 250;
        try (var executorService = Executors.newFixedThreadPool(threads)) {
            final var start = new CountDownLatch(1);
            final var futures = new ArrayList<Future<?>>();
            final var counter = new AtomicInteger();

            for (var thread = 0; thread < threads; thread++) {
                futures.add(executorService.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    for (var i = 0; i < perThread; i++) {
                        final var linkIdSuffix = counter.getAndIncrement();
                        final var userId = UUID.randomUUID();
                        final var link = mockLink("c" + linkIdSuffix, userId);
                        repository.saveLink(link);
                    }

                }));
            }
            start.countDown();

            for (final var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

            final var links = repository.findAll();
            assertEquals(threads * perThread, links.size());
        }
    }

    private Link mockLink(String code, UUID userId) {
        final var link = mock(Link.class);
        when(link.getCode()).thenReturn(code);
        when(link.getUserId()).thenReturn(userId);
        return link;
    }
}
