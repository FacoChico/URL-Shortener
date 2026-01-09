package com.mephi.skillfactory.urlshortener.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Component
public class CodeGenerator {
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    public String generate(String userUuid, String longLink, int length, int attempts) {
        try {
            final var md = MessageDigest.getInstance("SHA-256");
            md.update(userUuid.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(longLink.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(Integer.toString(attempts).getBytes(StandardCharsets.UTF_8));

            final var digest = md.digest();
            final var base62 = toBase62(digest);
            if (base62.length() >= length) {
                return base62.substring(0, length);
            }

            final var sb = new StringBuilder(base62);
            while (sb.length() < length) {
                sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
            }

            return sb.toString();
        } catch (Exception e) {
            // fallback
            final var sb = new StringBuilder(length);
            for (var i = 0; i < length; i++) {
                sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
            }

            return sb.toString();
        }
    }

    private String toBase62(byte[] bytes) {
        var bi = new BigInteger(1, bytes);
        if (bi.equals(BigInteger.ZERO)) {
            return String.valueOf(BASE62.charAt(0));
        }

        final var sb = new StringBuilder();
        final var base = BigInteger.valueOf(BASE62.length());
        while (bi.signum() > 0) {
            final var dr = bi.divideAndRemainder(base);
            sb.append(BASE62.charAt(dr[1].intValue()));
            bi = dr[0];
        }

        return sb.reverse().toString();
    }
}
