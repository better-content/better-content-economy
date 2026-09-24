package com.bettercontent.economy.mixin.client;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.trader.TraderCampPostRenderer;
import com.bettercontent.economy.trader.TraderCampRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TraderCampPostRendererRegistration {
    private TraderCampPostRendererRegistration() { }

    @SubscribeEvent
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TraderCampRegistries.POST_ENTITY.get(), TraderCampPostRenderer::new);
    }
}
