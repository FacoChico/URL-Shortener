package com.mephi.skillfactory.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeGeneratorUnitTest {
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    private final CodeGenerator generator = new CodeGenerator();

    @Test
    void shouldReturnCodeGivenLength() {
        final var length = 12;

        final var code = generator.generate(UUID.randomUUID().toString(), "https://example.com/some/path", length, 0);

        assertNotNull(code);
        assertEquals(length, code.length());
    }

    @Test
    void shouldReturnOnlyBase62Characters() {
        final var code = generator.generate(UUID.randomUUID().toString(), "https://example.com/charset-check", 16, 0);

        assertNotNull(code);
        assertTrue(BASE62_PATTERN.matcher(code).matches());
    }

    @Test
    void shouldReturnSameCodeForSameInput() {
        final var userId = UUID.randomUUID().toString();
        final var url = "https://example.com/some/path";
        final var length = 12;
        final var attempts = 0;

        final var code1 = generator.generate(userId, url, length, attempts);
        final var code2 = generator.generate(userId, url, length, attempts);

        assertNotNull(code1);
        assertNotNull(code2);
        assertEquals(code1, code2);
    }

    @Test
    void shouldReturnDifferentCodesForDifferentUsers() {
        final var url = "https://example.com/some/path";
        final var length = 10;
        final var attempts = 0;

        final var code1 = generator.generate(UUID.randomUUID().toString(), url, length, attempts);
        final var code2 = generator.generate(UUID.randomUUID().toString(), url, length, attempts);

        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
    }

    @Test
    void shouldReturnDifferentCodesForDifferentLinks() {
        final var userId = UUID.randomUUID().toString();
        final var length = 10;
        final var attempts = 0;

        final var code1 = generator.generate(userId, "https://aaaaa", length, attempts);
        final var code2 = generator.generate(userId, "https://bbbbb", length, attempts);

        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
    }

    @Test
    void shouldReturnDifferentCodesForDifferentAttempts() {
        final var userId = UUID.randomUUID().toString();
        final var url = "https://example.com/same";
        final var length = 12;

        final var code1 = generator.generate(userId, url, length, 0);
        final var code2 = generator.generate(userId, url, length, 1);

        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
    }

    @Test
    void shouldPreserveDeterministicPrefixIfDifferentLengthsGivenForSameOtherParams() {
        // given
        final var userId = UUID.randomUUID().toString();
        final var url = "https://example.com/prefix-test";

        // when
        final var shortCode = generator.generate(userId, url, 7, 0);
        final var longCode = generator.generate(userId, url, 100, 0);

        // then
        assertNotNull(shortCode);
        assertNotNull(longCode);
        assertEquals(7, shortCode.length());
        assertEquals(100, longCode.length());

        assertEquals(shortCode, longCode.substring(0, 7));
        assertTrue(BASE62_PATTERN.matcher(longCode).matches());
    }

    @Test
    void shouldGenerateWithoutCollisions() {
        // given
        final var n = 1000;
        final var seenCodes = new HashSet<>(n);
        String user;
        String url;
        String code;

        for (var i = 0; i < n; i++) {
            // when
            user = UUID.randomUUID().toString();
            url = "https://example.com/item/" + i;
            code = generator.generate(user, url, 12, 0);

            // then
            assertEquals(12, code.length());
            assertTrue(BASE62_PATTERN.matcher(code).matches());
            assertTrue(seenCodes.add(code));
        }
    }
}
