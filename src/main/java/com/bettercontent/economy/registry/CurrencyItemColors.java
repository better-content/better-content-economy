package com.bettercontent.economy.registry;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.spirit.EconomySpiritTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CurrencyItemColors {
    private CurrencyItemColors() {}

    @SubscribeEvent
    public static void register(RegisterColorHandlersEvent.Item event) {
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            int color = EconomySpiritTypes.type(identity).getItemColor().getRGB() & 0xFFFFFF;
            event.register((stack, tintIndex) -> color, CurrencyItems.item(identity).get());
        }
    }
}
