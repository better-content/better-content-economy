package com.bettercontent.economy.resident;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ResidentInteractions {
    private ResidentInteractions() {}

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity resident)
                || !ResidentRules.isResident(resident)) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        ResidentNetwork.open(player, resident);
    }
}
