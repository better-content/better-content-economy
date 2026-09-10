package com.bettercontent.economy.mixin.client;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.trader.WanderingTraderTheme;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selects the robe whose dominant color matches the trader's spirit market. */
@Mixin(WanderingTraderRenderer.class)
public abstract class WanderingTraderRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/npc/WanderingTrader;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$spiritRobe(
            final WanderingTrader trader, final CallbackInfoReturnable<ResourceLocation> callback) {
        WanderingTraderTheme theme = WanderingTraderTheme.fromId(
                trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG));
        if (theme != null) callback.setReturnValue(new ResourceLocation(
                BetterContentEconomy.MOD_ID, "textures/entity/wandering_trader/" + theme.id() + ".png"));
    }
}
