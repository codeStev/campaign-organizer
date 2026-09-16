package com.campaignorganizer.interchange.foundry.domain;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Deterministic 16-char {@code [A-Za-z0-9]} Foundry document/folder id from an
 * arbitrary stable key string (ADR-0115), so re-pushing the same source entity
 * always resolves to the same Foundry {@code _id} — the relay's {@code
 * keepId: true, override: true} then makes every push an idempotent in-place
 * replace instead of a duplicate. Does not need to match the unrelated
 * obsidianToFoundry project's JS {@code stableFoundryId()} bit-for-bit —
 * different namespace, same contract (deterministic, valid format,
 * collision-safe at personal scale).
 *
 * <p>SHA-256 of the key, interpreted as an unsigned 256-bit integer and
 * repeatedly reduced mod 62, consumes the whole digest and draws from the
 * full 62-symbol alphabet (~95 bits of entropy) — unlike truncating a hex
 * digest, which would only draw from 16 symbols and waste most of the
 * digest's collision resistance.
 */
public final class StableFoundryId {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; // 62 symbols
    private static final int LENGTH = 16;
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    private StableFoundryId() {
    }

    public static String from(String key) {
        byte[] digest = sha256(key.getBytes(StandardCharsets.UTF_8));
        BigInteger n = new BigInteger(1, digest); // unsigned, 256 bits
        StringBuilder out = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            BigInteger[] divRem = n.divideAndRemainder(BASE);
            out.append(ALPHABET.charAt(divRem[1].intValue()));
            n = divRem[0];
        }
        return out.toString();
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e); // never happens on any JVM
        }
    }
}
