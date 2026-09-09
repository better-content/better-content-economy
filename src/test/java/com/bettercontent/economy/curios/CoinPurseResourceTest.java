package com.bettercontent.economy.curios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class CoinPurseResourceTest {
    @Test
    void bottomStripKeepsAllSevenSlotsInsideTheVanillaInventoryWidth() {
        assertEquals(188, CoinPurseLayout.INVENTORY_HEIGHT + CoinPurseLayout.STRIP_HEIGHT);
        assertEquals(CoinPurseCurio.SLOT_COUNT, CoinPurseLayout.SLOT_COUNT);
        assertEquals(48, CoinPurseLayout.slotX(0));
        assertEquals(156, CoinPurseLayout.slotX(6));
        assertTrue(CoinPurseLayout.slotX(6) + 16 <= CoinPurseLayout.INVENTORY_WIDTH);
    }

    @Test
    void purseRetainsSevenSlotsAndVisibleNames() throws Exception {
        var loader = CoinPurseResourceTest.class.getClassLoader();
        try (var slotStream = loader.getResourceAsStream("data/better_content_economy/curios/slots/coin_purse.json");
                var langStream = loader.getResourceAsStream("assets/better_content_economy/lang/en_us.json")) {
            var slot = JsonParser.parseReader(new InputStreamReader(slotStream, StandardCharsets.UTF_8)).getAsJsonObject();
            var lang = JsonParser.parseReader(new InputStreamReader(langStream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(CoinPurseCurio.SLOT_COUNT, slot.get("size").getAsInt());
            assertEquals("Coin Purse", lang.get("curios.identifier.coin_purse").getAsString());
            assertTrue(lang.has("gui.better_content_economy.coin_purse"));
        }
    }
}
