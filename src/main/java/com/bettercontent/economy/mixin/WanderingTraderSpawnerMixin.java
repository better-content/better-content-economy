package com.bettercontent.economy.mixin;

import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderSpawner.class)
public abstract class WanderingTraderSpawnerMixin {
    @Shadow
    @Final
    private ServerLevelData serverLevelData;

    @Shadow
    protected abstract boolean spawn(ServerLevel level);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1)
    private void betterContentEconomy$runRecurringVisitSchedule(
            final ServerLevel level,
            final boolean spawnEnemies,
            final boolean spawnFriendlies,
            final CallbackInfoReturnable<Integer> callback) {
        if (!EconomyConfig.wanderingTraderRecurringVisits()) {
            return;
        }
        callback.setReturnValue(WanderingTraderVisits.tickScheduledVisit(
                level,
                serverLevelData,
                this::spawn));
    }

    @ModifyConstant(method = "spawn", constant = @Constant(intValue = 10), require = 1)
    private int betterContentEconomy$removeVanillaOneInTenGate(final int vanillaBound) {
        return EconomyConfig.wanderingTraderRecurringVisits() ? 1 : vanillaBound;
    }
}
