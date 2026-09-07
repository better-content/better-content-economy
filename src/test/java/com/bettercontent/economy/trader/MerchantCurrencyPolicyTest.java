package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class MerchantCurrencyPolicyTest {
    @Test
    void replacesEmeraldInEveryStackPositionWithoutChangingOtherData() {
        CompoundTag offer = offer("minecraft:emerald", "minecraft:emerald", "minecraft:emerald");
        offer.putInt("uses", 4);
        offer.putInt("maxUses", 9);
        offer.putFloat("priceMultiplier", 0.35F);

        assertTrue(MerchantCurrencyPolicy.replaceEmeraldIds(offer));
        assertEquals("createdeco:copper_coin", offer.getCompound("buy").getString("id"));
        assertEquals("createdeco:copper_coin", offer.getCompound("buyB").getString("id"));
        assertEquals("createdeco:copper_coin", offer.getCompound("sell").getString("id"));
        assertEquals(3, offer.getCompound("buy").getByte("Count"));
        assertEquals(4, offer.getCompound("buyB").getByte("Count"));
        assertEquals(5, offer.getCompound("sell").getByte("Count"));
        assertEquals(4, offer.getInt("uses"));
        assertEquals(9, offer.getInt("maxUses"));
        assertEquals(0.35F, offer.getFloat("priceMultiplier"));
    }

    @Test
    void leavesNonEmeraldAndAlreadyConvertedOffersAlone() {
        CompoundTag offer = offer("createdeco:copper_coin", "minecraft:diamond", "minecraft:apple");
        CompoundTag before = offer.copy();

        assertFalse(MerchantCurrencyPolicy.replaceEmeraldIds(offer));
        assertEquals(before, offer);
    }

    private static CompoundTag offer(String buy, String buyB, String sell) {
        CompoundTag offer = new CompoundTag();
        offer.put("buy", stack(buy, 3));
        offer.put("buyB", stack(buyB, 4));
        offer.put("sell", stack(sell, 5));
        return offer;
    }

    private static CompoundTag stack(String id, int count) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id);
        stack.putByte("Count", (byte) count);
        return stack;
    }
}
