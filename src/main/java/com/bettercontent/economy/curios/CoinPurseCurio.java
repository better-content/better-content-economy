package com.bettercontent.economy.curios;

import com.bettercontent.economy.BetterContentEconomy;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Dedicated Curios storage for the pack's direct-payment Create Deco coin items. */
public final class CoinPurseCurio {
    public static final ResourceLocation PREDICATE = new ResourceLocation(BetterContentEconomy.MOD_ID, "coin_purse");
    private static final Set<ResourceLocation> COINS = Set.of(
            new ResourceLocation("createdeco", "copper_coin"),
            new ResourceLocation("createdeco", "zinc_coin"),
            new ResourceLocation("createdeco", "iron_coin"),
            new ResourceLocation("createdeco", "industrial_iron_coin"),
            new ResourceLocation("createdeco", "brass_coin"),
            new ResourceLocation("createdeco", "gold_coin"),
            new ResourceLocation("createdeco", "netherite_coin"));

    private CoinPurseCurio() {}

    public static void registerPredicate() {
        CuriosApi.registerCurioPredicate(PREDICATE, result -> isCoin(result.stack()));
    }

    static boolean isCoin(final ItemStack stack) {
        return COINS.contains(stack.getItem().builtInRegistryHolder().key().location());
    }
}
