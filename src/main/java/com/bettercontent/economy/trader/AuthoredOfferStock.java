package com.bettercontent.economy.trader;

import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

/** Stable offer identity shared by authored merchants across save/reload. */
public final class AuthoredOfferStock {
    private AuthoredOfferStock() {}

    public static String key(final MerchantOffer offer) {
        return id(offer.getBaseCostA()) + ":" + id(offer.getCostB()) + "=>" + id(offer.getResult());
    }

    private static String id(final net.minecraft.world.item.ItemStack stack) {
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return (id == null ? "unknown" : id.toString()) + "#" + stack.getCount();
    }

    /** Results whose authored stock must remain finite instead of receiving vanilla restocks. */
    public static boolean isFiniteAuthoredResult(final ItemStack result) {
        if (result == null || result.isEmpty()) return false;
        return result.is(Items.DIAMOND) || result.is(Items.NETHERITE_SCRAP)
                || result.getItem() == Items.DIAMOND_SWORD || result.getItem() == Items.DIAMOND_PICKAXE
                || result.getItem() == Items.DIAMOND_AXE || result.getItem() == Items.DIAMOND_SHOVEL
                || result.getItem() == Items.DIAMOND_HOE || result.getItem() == Items.DIAMOND_HELMET
                || result.getItem() == Items.DIAMOND_CHESTPLATE || result.getItem() == Items.DIAMOND_LEGGINGS
                || result.getItem() == Items.DIAMOND_BOOTS;
    }
}
