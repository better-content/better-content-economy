package com.bettercontent.economy.trader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** Guaranteed one-time village-starting offer for every themed wandering trader. */
public final class VillageStarterOffer {
    public static final int COPPER_COST = 8;
    public static final int VILLAGER_COUNT = 2;

    private VillageStarterOffer() {}

    public static void ensurePresent(final MerchantOffers offers) {
        if (offers.stream().anyMatch(VillageStarterOffer::matches)) return;
        Item copper = ForgeRegistries.ITEMS.getValue(MerchantCurrencyPolicy.COPPER_COIN);
        if (copper != null) {
            offers.add(new MerchantOffer(
                    new ItemStack(copper, COPPER_COST),
                    new ItemStack(Items.VILLAGER_SPAWN_EGG, VILLAGER_COUNT),
                    1,
                    0,
                    0.0F));
        }
    }

    static boolean matches(final MerchantOffer offer) {
        ResourceLocation payment = ForgeRegistries.ITEMS.getKey(offer.getBaseCostA().getItem());
        return MerchantCurrencyPolicy.COPPER_COIN.equals(payment)
                && offer.getBaseCostA().getCount() == COPPER_COST
                && offer.getCostB().isEmpty()
                && offer.getResult().is(Items.VILLAGER_SPAWN_EGG)
                && offer.getResult().getCount() == VILLAGER_COUNT
                && offer.getMaxUses() == 1;
    }
}
