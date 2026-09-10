package com.bettercontent.economy.mixin;

import com.sammy.malum.core.handlers.SpiritHarvestHandler;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses Malum's equipment-gated death hook; SpiritAcquisition is the sole release authority. */
@Mixin(value = SpiritHarvestHandler.class, remap = false)
abstract class MalumSpiritHarvestMixin {
    @Inject(method = "spawnSpiritsOnDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void betterContentEconomy$usePlayerKillPolicy(
            final LivingDeathEvent event,
            final CallbackInfo callback) {
        callback.cancel();
    }
}
