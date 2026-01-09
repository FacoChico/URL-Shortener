package com.mephi.skillfactory.urlshortener.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;

@Service
public class NotificationService {
    private final ConcurrentHashMap<UUID, List<String>> notifications = new ConcurrentHashMap<>();

    public void notify(UUID user, String message) {
        notifications.compute(user, (k, v) -> {
            if (v == null) {
                v = synchronizedList(new ArrayList<>());
            }
            v.add(message);
            return v;
        });

        System.out.printf("NOTIFY %s: %s%n", user, message);
    }

    public List<String> getNotifications(UUID user) {
        return notifications.getOrDefault(user, emptyList());
    }
}
