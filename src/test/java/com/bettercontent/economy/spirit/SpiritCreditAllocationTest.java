package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SpiritCreditAllocationTest {
    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECIPIENT = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final long[] REGION_SEEDS = {11L, 37L, 101L, 202L, 999L};
    private static final int KILLS_PER_CASE = 128;

    @Test void sameNativeAspectConservesValueAndChangesCompositionByRegion() {
        var nativeUnits = java.util.Collections.nCopies(KILLS_PER_CASE, CurrencyIdentity.WORK);
        Map<CurrencyIdentity, Integer> baseline = null;
        boolean compositionChanged = false;
        for (long regionSeed : REGION_SEEDS) {
            Map<CurrencyIdentity, Integer> allocation = SpiritCreditAllocation.fromUnits(
                    nativeUnits, VICTIM, RECIPIENT, regionSeed);
            assertEquals(KILLS_PER_CASE, total(allocation), "region seed " + regionSeed);
            if (baseline == null) baseline = allocation;
            else if (!baseline.equals(allocation)) compositionChanged = true;
        }
        assertTrue(compositionChanged, "same native aspect should receive a measurable regional composition shift");
    }

    @Test void authoredAspectAndRegionCorpusConservesValueAndExposesEveryIdentity() {
        EnumMap<CurrencyIdentity, Integer> observed = new EnumMap<>(CurrencyIdentity.class);
        int cases = 0;
        for (CurrencyIdentity nativeAspect : CurrencyIdentity.values()) {
            var nativeUnits = java.util.Collections.nCopies(KILLS_PER_CASE, nativeAspect);
            for (long regionSeed : REGION_SEEDS) {
                Map<CurrencyIdentity, Integer> allocation = SpiritCreditAllocation.fromUnits(
                        nativeUnits, VICTIM, RECIPIENT, regionSeed);
                assertEquals(KILLS_PER_CASE, total(allocation),
                        "native aspect " + nativeAspect + " in region " + regionSeed);
                allocation.forEach((identity, count) -> observed.merge(identity, count, Integer::sum));
                cases++;
            }
        }

        assertEquals(CurrencyIdentity.values().length, observed.size(),
                "all economy identities should be obtainable across the authored aspect/region corpus");
        assertEquals(KILLS_PER_CASE * cases, total(observed));
    }

    private static int total(Map<CurrencyIdentity, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }
}
