package com.bettercontent.economy.curios;

import com.bettercontent.economy.BetterContentEconomy;
import java.util.Set;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Dedicated Curios storage for the pack's direct-payment Create Deco coin items. */
public final class CoinPurseCurio {
    public static final String SLOT_ID = "coin_purse";
    public static final int SLOT_COUNT = 7;
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

    public static boolean isCoin(final ItemStack stack) {
        return COINS.contains(stack.getItem().builtInRegistryHolder().key().location());
    }

    public static Optional<IDynamicStackHandler> stacks(final Player player) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(SLOT_ID))
                .map(handler -> handler.getStacks());
    }

    /** Inserts matching denominations before empty purse slots, returning any remainder. */
    public static ItemStack insert(final Player player, final ItemStack stack) {
        if (!isCoin(stack)) return stack;
        return stacks(player)
                .map(handler -> insertInto(handler, stack))
                .orElse(stack);
    }

    public static ItemStack insertInto(final IItemHandler handler, final ItemStack stack) {
        if (!isCoin(stack)) return stack;
        return ItemHandlerHelper.insertItemStacked(handler, stack, false);
    }
}
