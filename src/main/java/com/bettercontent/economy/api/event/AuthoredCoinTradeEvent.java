package com.bettercontent.economy.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/** Posted after a completed trade consumes a coin through an economy-authored trader. */
public final class AuthoredCoinTradeEvent extends Event {
    private final ServerPlayer player;

    public AuthoredCoinTradeEvent(ServerPlayer player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public ServerPlayer getPlayer() { return player; }
}
