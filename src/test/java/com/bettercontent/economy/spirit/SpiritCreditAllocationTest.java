package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SpiritCreditAllocationTest {
    @Test void regionChangesCompositionButEveryUnitRemainsAccountedFor() {
        var units = java.util.Collections.nCopies(128, CurrencyIdentity.WORK);
        var a = SpiritCreditAllocation.fromUnits(units, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"), 11L);
        var b = SpiritCreditAllocation.fromUnits(units, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"), 99L);
        assertEquals(128, a.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(128, b.values().stream().mapToInt(Integer::intValue).sum());
        assertNotEquals(a, b);
    }
    @Test void manyKillsKeepAStableRegionalBiasAndConserveEveryUnit() {
        var units = java.util.Collections.nCopies(800, CurrencyIdentity.WORK);
        var regionA = SpiritCreditAllocation.fromUnits(units, UUID.randomUUID(), UUID.randomUUID(), 101L);
        var regionB = SpiritCreditAllocation.fromUnits(units, UUID.randomUUID(), UUID.randomUUID(), 202L);
        assertEquals(800, regionA.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(800, regionB.values().stream().mapToInt(Integer::intValue).sum());
        assertNotEquals(regionA, regionB);
    }
}
