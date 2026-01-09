package com.mephi.skillfactory.urlshortener.cli;

import com.mephi.skillfactory.urlshortener.service.NotificationService;
import com.mephi.skillfactory.urlshortener.service.ShortLinkService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.Scanner;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {
    private final ShortLinkService shortLinkService;
    private final NotificationService notificationService;
    private volatile UUID currentUserId;

    @Override
    public void run(String... args) {
        final var cliThread = new Thread(this::cliInteractionLoop, "url-shortener-cli");
        cliThread.setDaemon(true);
        cliThread.start();
    }

    private void cliInteractionLoop() {
        final var sc = new Scanner(System.in);
        printWelcome();

        while (true) {
            try {
                System.out.print("> ");
                if (!sc.hasNextLine()) {
                    break;
                }
                final var line = sc.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                final var parts = line.split("\\s+", 2);
                final var cmd = parts[0].toLowerCase();
                final var arg = parts.length > 1
                    ? parts[1].trim()
                    : "";

                switch (cmd) {
                    case "help", "h" -> printHelp();
                    case "uid" -> System.out.printf("Current user id: %s%n", currentUserId == null
                        ? "<not set>"
                        : currentUserId);
                    case "setuid" -> setUser(arg);
                    case "create" -> createNewShortLink(arg, sc);
                    case "open" -> openByCode(arg);
                    case "list" -> listUserLinks();
                    case "delete" -> deleteLink(arg);
                    case "notifications" -> showNotifications();
                    default -> System.out.println("Unknown command. Type 'help' for commands");
                }
            } catch (Exception e) {
                System.out.println("CLI error: " + e.getMessage());
            }
        }
    }

    private void printWelcome() {
        System.out.println("URL Shortener CLI ready. Type 'help' for commands");
        System.out.println("Note: user id is kept in memory and is lost on process restart");
    }

    private void printHelp() {
        System.out.println("""
            Commands:
              create <url>      — create new short link
              open <code>       — open short link in browser
              list              — list your links
              delete <code>     — delete your link
              notifications     — list your notifications
              uid               — show current user id
              setuid <id|new>   — set current user id or generate new one
            """);
    }

    private void setUser(String arg) {
        if (arg == null || arg.isBlank()) {
            System.out.println("Usage: setuid <id|new>");
            return;
        }

        if ("new".equalsIgnoreCase(arg)) {
            currentUserId = UUID.randomUUID();
            System.out.println("Generated new user id: " + currentUserId);
            return;
        }

        try {
            currentUserId = UUID.fromString(arg);
            System.out.println("Switched to user id: " + currentUserId);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format");
        }
    }

    private void createNewShortLink(String arg, Scanner sc) {
        var url = arg;
        if (url == null || url.isBlank()) {
            System.out.print("Enter URL to shorten: ");
            url = sc.nextLine().trim();
            if (url.isEmpty()) {
                System.out.println("Aborted: empty URL");
                return;
            }
        }

        Integer maxClicks = null;
        Long ttlSeconds = null;

        try {
            System.out.print("Max clicks (enter for default): ");
            final var sMax = sc.nextLine().trim();
            if (!sMax.isEmpty()) {
                maxClicks = Integer.parseInt(sMax);
            }

            System.out.print("TTL seconds (enter for default): ");
            final var sTtl = sc.nextLine().trim();
            if (!sTtl.isEmpty()) {
                ttlSeconds = Long.parseLong(sTtl);
            }
        } catch (NumberFormatException e) {
            System.out.printf("Invalid number: %s. Aborting creation%n", e.getMessage());
            return;
        }

        try {
            final var shortLink = shortLinkService.createShortLink(url, currentUserId, maxClicks, ttlSeconds);
            final var userId = shortLink.getUserId();
            if (currentUserId == null) {
                currentUserId = userId;
                System.out.println("A new user id was generated by service: " + currentUserId);
            }

            System.out.println("Created: " + shortLink.getCode());
            System.out.println("Short URL: " + shortLinkService.constructShortLinkUrl(shortLink));
        } catch (Exception e) {
            System.out.println("Failed to create short link: " + e.getMessage());
        }
    }

    private void openByCode(String code) {
        if (code == null || code.isBlank()) {
            System.out.println("Usage: open <code>");
            return;
        }

        final var linkOptional = shortLinkService.getShortLink(code);
        if (linkOptional.isEmpty()) {
            System.out.println("Link not found / expired / inactive: " + code);
            return;
        }

        final var url = linkOptional.get().getTargetUrl();

        try {
            if (!Desktop.isDesktopSupported()) {
                System.out.println("Desktop is not supported in this environment");
                System.out.println("Target URL: " + url);
                return;
            }

            final var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.out.println("Browse action is not supported");
                System.out.println("Target URL: " + url);
                return;
            }

            desktop.browse(new URI(url));
            System.out.println("Opened in browser: " + url);
        } catch (java.awt.HeadlessException e) {
            System.out.println("Headless environment detected. Cannot open browser automatically");
            System.out.println("Target URL: " + url);
        } catch (Exception e) {
            System.out.println("Failed to open browser: " + e.getMessage());
            System.out.println("Target URL: " + url);
        }
    }

    private void listUserLinks() {
        if (currentUserId == null) {
            System.out.println("No user id set — you have no links. Create a link first or use setuid");
            return;
        }

        try {
            final var links = shortLinkService.listByUserId(currentUserId);
            if (links.isEmpty()) {
                System.out.println("You have no links");
                return;
            }

            System.out.println("Your links:");
            for (final var link : links) {
                System.out.printf("  %s -> %s | clicks=%d/%s | created=%s | active=%s%n", link.getCode(), link.getTargetUrl(), link.getClickCount()
                    .get(), link.getMaxClicks(), link.getCreatedAt(), link.isActive());
            }
        } catch (Exception e) {
            System.out.println("Failed to list links: " + e.getMessage());
        }
    }

    private void deleteLink(String code) {
        if (code == null || code.isBlank()) {
            System.out.println("Usage: delete <code>");
            return;
        }
        if (currentUserId == null) {
            System.out.println("No user id set — cannot delete. Use setuid or create a link first");
            return;
        }

        try {
            if (shortLinkService.deleteLink(code, currentUserId)) {
                System.out.println("Deleted: " + code);
            } else {
                System.out.println("Link not found: " + code);
            }
        } catch (SecurityException e) {
            System.out.println("Forbidden: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Failed to delete: " + e.getMessage());
        }
    }

    private void showNotifications() {
        if (currentUserId == null) {
            System.out.println("No user id set — no notifications available. Use setuid or create a link first");
            return;
        }

        try {
            final var notifications = notificationService.getNotifications(currentUserId);
            if (notifications == null || notifications.isEmpty()) {
                System.out.println("No notifications");
                return;
            }
            System.out.println("Notifications:");
            for (final var notification : notifications) {
                System.out.println("  — " + notification);
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch notifications: " + e.getMessage());
        }
    }
}
