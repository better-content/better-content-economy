package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterContentEconomy.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LocalMarketClient {
    private LocalMarketClient() {}

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) return;
        int buttonWidth = Math.min(78, screen.width);
        int buttonX = Math.max(0, Math.min(screen.width - buttonWidth, screen.width / 2 + 90));
        int buttonY = Math.max(0, Math.min(screen.height - 20, screen.height / 2 - 82));
        event.addListener(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("screen.better_content_economy.local_market"), button -> {
                    LocalMarketNetwork.request();
                    Minecraft.getInstance().setScreen(new LocalMarketScreen(screen));
                }).bounds(buttonX, buttonY, buttonWidth, 20).build());
    }

    static void receive(LocalMarketNetwork.Snapshot snapshot) {
        if (Minecraft.getInstance().screen instanceof LocalMarketScreen screen) screen.update(snapshot.rows());
    }
}
