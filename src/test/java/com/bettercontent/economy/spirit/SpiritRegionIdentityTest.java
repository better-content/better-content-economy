package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SpiritRegionIdentityTest {
    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECIPIENT = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void identityAndSeedAreStableForAnEcologyAndDistinctAcrossBiomeOrDimension() {
        var forest = SpiritRegionIdentity.of("minecraft:overworld", "minecraft:forest");
        var sameForest = SpiritRegionIdentity.of("minecraft:overworld", "minecraft:forest");
        var desert = SpiritRegionIdentity.of("minecraft:overworld", "minecraft:desert");
        var otherDimension = SpiritRegionIdentity.of("minecraft:the_nether", "minecraft:forest");

        assertEquals("minecraft:overworld:minecraft:forest", forest.key());
        assertEquals(forest.seed(), sameForest.seed());
        assertNotEquals(forest.seed(), desert.seed());
        assertNotEquals(forest.seed(), otherDimension.seed());
    }

    @Test void aNativeCohortGetsARepeatableBiomeSpecificAspectMixWithoutChangingTotal() {
        var nativeWork = Collections.nCopies(128, CurrencyIdentity.WORK);
        long forestSeed = SpiritRegionIdentity.of("minecraft:overworld", "minecraft:forest").seed();
        long desertSeed = SpiritRegionIdentity.of("minecraft:overworld", "minecraft:desert").seed();

        Map<CurrencyIdentity, Integer> forest = SpiritCreditAllocation.fromUnits(nativeWork, VICTIM, RECIPIENT, forestSeed);
        Map<CurrencyIdentity, Integer> sameForest = SpiritCreditAllocation.fromUnits(nativeWork, VICTIM, RECIPIENT, forestSeed);
        Map<CurrencyIdentity, Integer> desert = SpiritCreditAllocation.fromUnits(nativeWork, VICTIM, RECIPIENT, desertSeed);

        assertEquals(forest, sameForest);
        assertNotEquals(forest, desert);
        assertEquals(128, forest.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(128, desert.values().stream().mapToInt(Integer::intValue).sum());
        assertNotNull(forest.get(CurrencyIdentity.TEMPO));
    }
}
