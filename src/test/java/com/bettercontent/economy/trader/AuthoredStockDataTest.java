package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class AuthoredStockDataTest {
    @Test void pickupGateRejectsDepletedAndInsufficientStockWithoutMutation() {
        String key = offerKey("diamond", 2);
        AuthoredStockData stock = AuthoredStockData.createForTest(Map.of(key, 1));

        assertFalse(stock.canPurchase(key, 2));
        assertFalse(stock.recordPurchase(key, 2));
        assertEquals(1, stock.remaining(key));
    }

    @Test void completedPurchaseIsPersistedAndStaleMenuCheckSeesExhaustion() {
        String key = offerKey("diamond", 2);
        AuthoredStockData stock = AuthoredStockData.createForTest(Map.of(key, 2));

        assertTrue(stock.canPurchase(key, 2));
        assertTrue(stock.recordPurchase(key, 2));
        assertFalse(stock.canPurchase(key, 2));
        AuthoredStockData reloaded = AuthoredStockData.load(stock.save(new CompoundTag()));
        assertFalse(reloaded.canPurchase(key, 2));
        assertEquals(0, reloaded.remaining(key));
    }

    @Test void unseededCommodityRemainsUntouchedRatherThanBecomingZeroStock() {
        AuthoredStockData stock = AuthoredStockData.createForTest(Map.of("minecraft:diamond", 4));

        assertTrue(stock.canPurchase("minecraft:netherite_ingot", 1));
        assertTrue(stock.recordPurchase("minecraft:netherite_ingot", 1));
        assertEquals(0, stock.remaining("minecraft:netherite_ingot"));
        assertEquals(4, stock.remaining("minecraft:diamond"));
    }

    @Test void legacyOfferBalancesMigrateAndCoalesceByPhysicalOutput() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("minecraft:emerald#9:unknown#0=>minecraft:diamond#1", 2);
        legacy.putInt("better_content_economy:tempo_spirit#2:unknown#0=>minecraft:diamond#1", 3);
        CompoundTag root = new CompoundTag();
        root.putInt("version", 1);
        root.put("stock", legacy);

        AuthoredStockData migrated = AuthoredStockData.load(root);

        assertEquals(5, migrated.remaining("minecraft:diamond"));
        AuthoredStockData reloaded = AuthoredStockData.load(migrated.save(new CompoundTag()));
        assertEquals(5, reloaded.remaining("minecraft:diamond"));
        assertTrue(reloaded.canPurchase("minecraft:diamond", 1));
    }

    private static String offerKey(final String result, final int count) {
        return AuthoredOfferStock.key(new ResourceLocation("minecraft", result));
    }
}
