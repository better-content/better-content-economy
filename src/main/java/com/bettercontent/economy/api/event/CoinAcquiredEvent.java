package com.bettercontent.economy.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;
import java.util.UUID;

/** Retained binary API for old consumers; the spirit economy never posts this event. */
@Deprecated(forRemoval = false)
public final class CoinAcquiredEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation denomination;
    private final UUID itemEntityId;

    public CoinAcquiredEvent(ServerPlayer player, ResourceLocation denomination, UUID itemEntityId) {
        this.player = Objects.requireNonNull(player, "player");
        this.denomination = Objects.requireNonNull(denomination, "denomination");
        this.itemEntityId = Objects.requireNonNull(itemEntityId, "itemEntityId");
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getDenomination() { return denomination; }
    public UUID getItemEntityId() { return itemEntityId; }

    public String getEpisodeId() { return player.getUUID() + ":coin:" + itemEntityId; }
}
