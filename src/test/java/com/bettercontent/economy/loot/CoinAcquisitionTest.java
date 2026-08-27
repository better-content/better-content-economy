package com.bettercontent.economy.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class CoinAcquisitionTest {
    @Test void catalogueOwnsEveryTieredChestTable() {
        assertEquals(31, CoinAcquisition.chestTableCount());
    }
}
