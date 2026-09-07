package com.bettercontent.economy.trader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** Converts exact emerald stacks in external merchant offers without changing offer metadata. */
public final class MerchantCurrencyPolicy {
    static final ResourceLocation EMERALD = new ResourceLocation("minecraft", "emerald");
    static final ResourceLocation COPPER_COIN = new ResourceLocation("createdeco", "copper_coin");
    private static final String[] STACK_KEYS = {"buy", "buyB", "sell"};

    private MerchantCurrencyPolicy() {}

    public static void normalize(final MerchantOffers offers) {
        for (int index = 0; index < offers.size(); index++) {
            MerchantOffer offer = offers.get(index);
            CompoundTag tag = offer.createTag();
            if (replaceEmeraldIds(tag)) offers.set(index, new MerchantOffer(tag));
        }
    }

    static boolean replaceEmeraldIds(final CompoundTag offerTag) {
        boolean changed = false;
        for (String key : STACK_KEYS) {
            CompoundTag stack = offerTag.getCompound(key);
            if (EMERALD.toString().equals(stack.getString("id"))) {
                stack.putString("id", COPPER_COIN.toString());
                changed = true;
            }
        }
        return changed;
    }

    public static boolean isExternalMerchantType(final net.minecraft.world.entity.Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && !"minecraft".equals(id.getNamespace());
    }
}
