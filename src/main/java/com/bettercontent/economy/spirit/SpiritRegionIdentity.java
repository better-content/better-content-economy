package com.bettercontent.economy.spirit;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Stable biome-and-dimension identity shared by spirit allocation and regional observation. */
public final class SpiritRegionIdentity {
    private final String dimensionId;
    private final String biomeId;
    private final String key;
    private final long seed;

    private SpiritRegionIdentity(final String dimensionId, final String biomeId) {
        this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        this.biomeId = Objects.requireNonNull(biomeId, "biomeId");
        this.key = dimensionId + ":" + biomeId;
        this.seed = stableHash(key);
    }

    public static SpiritRegionIdentity of(final String dimensionId, final String biomeId) {
        return new SpiritRegionIdentity(dimensionId, biomeId);
    }

    public String dimensionId() { return dimensionId; }
    public String biomeId() { return biomeId; }
    public String key() { return key; }
    public long seed() { return seed; }

    private static long stableHash(final String value) {
        long hash = 0xcbf29ce484222325L;
        for (byte part : value.getBytes(StandardCharsets.UTF_8)) {
            hash = (hash ^ (part & 0xffL)) * 0x100000001b3L;
        }
        return hash;
    }
}
