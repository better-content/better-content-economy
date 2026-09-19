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
}
