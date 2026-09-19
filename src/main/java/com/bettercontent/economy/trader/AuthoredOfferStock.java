package com.bettercontent.economy.trader;

import net.minecraft.world.item.trading.MerchantOffer;

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

}
