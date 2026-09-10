package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class VillagerCatalogueTest {
    @Test void catalogueHasThirtyFiveRowsForEverySpiritProfession() {
        assertEquals(245, VillagerCatalogue.rowCount());
        assertTrue(VillagerCatalogue.allRowsUseExactlyOneSpiritSide());
    }
}
