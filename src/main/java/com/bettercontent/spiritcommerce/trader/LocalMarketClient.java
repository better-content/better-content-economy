package com.bettercontent.spiritcommerce.trader;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import com.bettercontent.spiritcommerce.mixin.client.MerchantScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterSpiritCommerce.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LocalMarketClient {
    private static LocalMarketNetwork.SelectedOffer pendingSelection;
    private static int pendingTicks;
    private LocalMarketClient() {}

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) return;
        int buttonWidth = Math.min(78, screen.width);
        int buttonX = Math.max(0, Math.min(screen.width - buttonWidth, screen.width / 2 + 90));
        int buttonY = Math.max(0, Math.min(screen.height - 20, screen.height / 2 - 106));
        event.addListener(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("screen.better_spirit_commerce.local_market"),
                button -> openFromMerchant(screen)).bounds(buttonX, buttonY, buttonWidth, 20).build());
    }

    static void openFromMerchant(MerchantScreen screen) {
        LocalMarketNetwork.request();
        Minecraft.getInstance().setScreen(new LocalMarketScreen(screen));
    }

    static void receive(LocalMarketNetwork.Snapshot snapshot) {
        if (Minecraft.getInstance().screen instanceof LocalMarketScreen screen) screen.update(snapshot);
    }

    static void selectOpenedOffer(LocalMarketNetwork.SelectedOffer selected) {
        pendingSelection = selected;
        pendingTicks = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingSelection == null) return;
        if (++pendingTicks > 100) {
            pendingSelection = null;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof MerchantScreen screen)
                || screen.getMenu().containerId != pendingSelection.containerId()
                || screen.getMenu().getOffers().size() <= pendingSelection.offerIndex()) return;
        int index = pendingSelection.offerIndex();
        ((MerchantScreenAccessor) screen).bettercontent$setShopItem(index);
        ((MerchantScreenAccessor) screen).bettercontent$setScrollOff(Math.max(0, index - 3));
        screen.getMenu().setSelectionHint(index);
        screen.getMenu().tryMoveItems(index);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundSelectTradePacket(index));
        }
        pendingSelection = null;
    }
}
