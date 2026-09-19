package com.bettercontent.economy.ops;

import static org.junit.jupiter.api.Assertions.*;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ObservationalSpiritExportTest {
    @Test void exportIsOptInAndPermissionGated() {
        var snapshot = new ObservationalSpiritExport.CreditSnapshot(Map.of(CurrencyIdentity.IMPACT, 4), Map.of());
        assertEquals("", ObservationalSpiritExport.csv(false, 4, List.of(snapshot), List.of()));
        assertEquals("", ObservationalSpiritExport.json(true, 1, List.of(snapshot), List.of()));
    }

    @Test void exportConservesIssuedAndPendingTotals() {
        var snapshot = new ObservationalSpiritExport.CreditSnapshot(Map.of(CurrencyIdentity.IMPACT, 4), Map.of(CurrencyIdentity.TEMPO, 3));
        String json = ObservationalSpiritExport.json(true, 2, List.of(snapshot), List.of());
        assertTrue(json.contains("\"issued\":4"));
        assertTrue(json.contains("\"pending\":3"));
    }

    @Test void exchangesContainOnlyExplicitSuccessfulObservations() {
        var exchange = new ObservationalSpiritExport.Exchange(CurrencyIdentity.WORK, 2, "minecraft:villager");
        String csv = ObservationalSpiritExport.csv(true, 2, List.of(), List.of(exchange));
        assertTrue(csv.contains("exchange,work,2,minecraft:villager"));
        assertFalse(csv.contains("motive"));
        assertFalse(csv.contains("private"));
    }
}
