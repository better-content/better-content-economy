package com.bettercontent.economy.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class AuthoredSpiritTradeEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation spiritId;
    private final int cost;
    private final ResourceLocation merchantTheme;

    public AuthoredSpiritTradeEvent(ServerPlayer player, ResourceLocation spiritId, int cost, ResourceLocation merchantTheme) {
        this.player = player;
        this.spiritId = spiritId;
        this.cost = cost;
        this.merchantTheme = merchantTheme;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getSpiritId() { return spiritId; }
    public int getCost() { return cost; }
    public ResourceLocation getMerchantTheme() { return merchantTheme; }
}
