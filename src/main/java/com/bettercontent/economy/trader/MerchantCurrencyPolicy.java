package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** Removes unsupported coin and emerald offers from external merchant implementations. */
public final class MerchantCurrencyPolicy {
    private static final ResourceLocation EMERALD = new ResourceLocation("minecraft", "emerald");

    private MerchantCurrencyPolicy() {}

    public static void normalize(final MerchantOffers offers) {
        offers.removeIf(MerchantCurrencyPolicy::containsRetiredCurrency);
    }

    private static boolean containsRetiredCurrency(final MerchantOffer offer) {
        return isRetiredCurrency(offer.getBaseCostA()) || isRetiredCurrency(offer.getCostB())
                || isRetiredCurrency(offer.getResult());
    }

    private static boolean isRetiredCurrency(final ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return EMERALD.equals(id) || (id != null && EconomyPolicy.isRetired(id));
    }

    public static boolean isExternalMerchantType(final Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && !"minecraft".equals(id.getNamespace());
    }
}
