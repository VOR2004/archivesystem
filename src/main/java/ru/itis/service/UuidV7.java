package ru.itis.service;

import java.security.SecureRandom;
import java.util.UUID;

final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    static UUID generate() {
        // RFC 9562: 48-bit Unix timestamp, version 7, 74 random bits, IETF variant.
        long mostSignificant = (System.currentTimeMillis() << 16)
                | 0x7000L | (RANDOM.nextInt(1 << 12));
        long leastSignificant = (RANDOM.nextLong() & 0x3fff_ffff_ffff_ffffL)
                | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
