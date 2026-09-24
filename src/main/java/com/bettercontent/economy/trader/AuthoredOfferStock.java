package com.bettercontent.economy.trader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.trading.MerchantOffer;

/** Shared inventory identity for a physical result commodity, independent of price or seller. */
public final class AuthoredOfferStock {
    private AuthoredOfferStock() {}

    public static String key(final MerchantOffer offer) {
        return key(id(offer.getResult()));
    }

    static String key(final ResourceLocation commodity) {
        return commodity == null ? "unknown" : commodity.toString();
    }

    static String key(final ResourceLocation costA, final int costACount,
                      final ResourceLocation costB, final int costBCount,
                      final ResourceLocation result, final int resultCount) {
        return key(result);
    }

    private static ResourceLocation id(final net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return null;
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
    }
}
