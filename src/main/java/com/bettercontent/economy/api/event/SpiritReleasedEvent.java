package com.bettercontent.economy.api.event;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A credited kill has successfully added this native spirit entity to the world. */
public final class SpiritReleasedEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation spiritId;
    private final int count;
    private final UUID spiritEntityUuid;

    public SpiritReleasedEvent(ServerPlayer player, ResourceLocation spiritId, int count, UUID spiritEntityUuid) {
        this.player = player;
        this.spiritId = spiritId;
        this.count = count;
        this.spiritEntityUuid = spiritEntityUuid;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getSpiritId() { return spiritId; }
    public int getCount() { return count; }
    public UUID getSpiritEntityUuid() { return spiritEntityUuid; }
}
