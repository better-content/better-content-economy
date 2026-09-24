package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class AuthoredOfferStockTest {
    @Test void pricesAndOutputStackSizesShareOnePhysicalCommodityKey() {
        ResourceLocation diamond = new ResourceLocation("minecraft", "diamond");
        String firstPrice = AuthoredOfferStock.key(new ResourceLocation("minecraft", "emerald"), 6,
                null, 0, diamond, 1);
        String alternatePrice = AuthoredOfferStock.key(new ResourceLocation("better_content_economy", "tempo_spirit"), 11,
                new ResourceLocation("minecraft", "emerald"), 3, diamond, 2);

        assertEquals("minecraft:diamond", firstPrice);
        assertEquals(firstPrice, alternatePrice);
    }
}
