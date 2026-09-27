package com.bettercontent.economy.visualharness;

import com.bettercontent.economy.trader.LocalMarketScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Captures the visible production GUI after its server snapshot has settled. */
final class MarketVisualScreenshot {
    private static String pending;
    private static int settleTicks;
    private static boolean installed;

    private MarketVisualScreenshot() {}

    static void request(String name) {
        if (!installed) {
            MinecraftForge.EVENT_BUS.register(MarketVisualScreenshot.class);
            installed = true;
        }
        Minecraft.getInstance().options.hideGui = false;
        pending = name + ".png";
        settleTicks = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if ((pending.equals("market-selected.png") ? !(minecraft.screen instanceof MerchantScreen)
                : !(minecraft.screen instanceof LocalMarketScreen)) || ++settleTicks < 30) return;
        String fileName = pending;
        pending = null;
        Screenshot.grab(minecraft.gameDirectory, fileName, minecraft.getMainRenderTarget(), message -> {
            Path output = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(fileName);
            if (!Files.isRegularFile(output)) {
                throw new IllegalStateException("Market visual screenshot failed: " + message.getString());
            }
            System.out.println("MARKET_VISUAL captured " + output.toAbsolutePath());
        });
    }
}
