package com.mephi.skillfactory.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceUnitTest {
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    void shouldReturnEmptyListIfNotificationsEmpty() {
        final var notes = notificationService.getNotifications(UUID.randomUUID());

        assertNotNull(notes);
        assertTrue(notes.isEmpty());

        // assert returned list is immutable
        assertThrows(UnsupportedOperationException.class, () -> notes.add("x"));
    }

    @Test
    void shouldReturnNotifications() {
        final var userId = UUID.randomUUID();

        notificationService.notify(userId, "test");

        final var notifications = notificationService.getNotifications(userId);
        assertEquals(1, notifications.size());
        assertEquals("test", notifications.getFirst());
    }

    @Test
    void shouldPreserveNotificationsOrder() {
        final var userId = UUID.randomUUID();

        notificationService.notify(userId, "first");
        notificationService.notify(userId, "second");
        notificationService.notify(userId, "third");

        final var notifications = notificationService.getNotifications(userId);

        assertEquals(List.of("first", "second", "third"), notifications);
    }

    @Test
    void shouldIsolateNotificationsBetweenDifferentUsers() {
        // given
        final var user1 = UUID.randomUUID();
        final var user2 = UUID.randomUUID();

        notificationService.notify(user1, "user1-1");
        notificationService.notify(user2, "user2-1");
        notificationService.notify(user1, "user1-2");

        // when
        final var notifications1 = notificationService.getNotifications(user1);
        final var notifications2 = notificationService.getNotifications(user2);

        // then
        assertEquals(2, notifications1.size());
        assertTrue(notifications1.contains("user1-1"));
        assertTrue(notifications1.contains("user1-2"));

        assertEquals(1, notifications2.size());
        assertEquals(List.of("user2-1"), notifications2);
    }
}
