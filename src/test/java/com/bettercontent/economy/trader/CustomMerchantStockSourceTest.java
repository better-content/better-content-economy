package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CustomMerchantStockSourceTest {
    @Test
    void vanillaMerchantResultSlotGuardsAndDebitsNonVillagerMerchants() throws IOException {
        final String guard = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/mixin/AuthoredStockSlotMixin.java"));
        final String debit = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/mixin/AuthoredMerchantResultSlotMixin.java"));
        final String config = Files.readString(Path.of(
                "src/main/resources/better_content_economy.mixins.json"));

        assertTrue(guard.contains("AuthoredMerchantStock.canPickup(merchant, offer, level)"));
        assertTrue(debit.contains("@Mixin(MerchantResultSlot.class)"));
        assertTrue(debit.contains("Merchant;notifyTrade"));
        assertTrue(debit.contains("AuthoredMerchantStock.recordCustomMerchantTrade(merchant, offer, level)"));
        assertTrue(config.contains("AuthoredMerchantResultSlotMixin"));
    }
}
