package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class VillagerCatalogueTest {
    @Test void cataloguePreservesEveryAuthoredTradeAndCoinBoundary() {
        assertEquals(312, VillagerCatalogue.rowCount());
        assertTrue(VillagerCatalogue.allRowsUseExactlyOneCoinSide());
    }
}
