package com.bettercontent.economy.curios;

import com.bettercontent.economy.compat.ThreadSignalsBridge;
import com.bettercontent.economy.mixin.ItemEntityAccessor;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Routes direct-payment coin entities through the purse before the ordinary inventory. */
public final class CoinPickupRouter {
    private CoinPickupRouter() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void route(final EntityItemPickupEvent event) {
        Player player = event.getEntity();
        ItemEntity entity = event.getItem();
        ItemStack offered = entity.getItem();
        if (player.level().isClientSide || !CoinPurseCurio.isCoin(offered)) return;

        UUID target = ((ItemEntityAccessor) entity).betterContentEconomy$getTarget();
        if (target != null && !target.equals(player.getUUID())) return;

        Item item = offered.getItem();
        ItemStack remainder = CoinPurseCurio.insert(player, offered.copy());
        player.getInventory().add(remainder);
        int accepted = offered.getCount() - remainder.getCount();
        if (accepted <= 0) return;

        ItemStack pickedUp = offered.copyWithCount(accepted);
        entity.setItem(remainder);
        ForgeEventFactory.firePlayerItemPickupEvent(player, entity, pickedUp);
        player.take(entity, accepted);
        if (remainder.isEmpty()) entity.discard();
        player.awardStat(Stats.ITEM_PICKED_UP.get(item), accepted);
        player.onItemPickup(entity);
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            ThreadSignalsBridge.coinAcquired(
                    serverPlayer,
                    item.builtInRegistryHolder().key().location(),
                    entity.getUUID());
        }
    }
}
