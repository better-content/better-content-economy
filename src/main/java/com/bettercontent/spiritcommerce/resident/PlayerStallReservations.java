package com.bettercontent.spiritcommerce.resident;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Prevents one resident from answering several player stalls at once. */
final class PlayerStallReservations {
    private record Visit(String dimension, BlockPos stall, long until) {}
    private static final Map<UUID, Visit> VISITS = new HashMap<>();
    private PlayerStallReservations() {}

    static boolean reserve(ServerLevel level, UUID resident, BlockPos stall, long now) {
        Visit active = VISITS.get(resident);
        if (active != null && active.until() > now) return false;
        VISITS.put(resident, new Visit(level.dimension().location().toString(), stall.immutable(), now + 210));
        return true;
    }

    static void release(ServerLevel level, UUID resident, BlockPos stall) {
        Visit active = VISITS.get(resident);
        if (active != null && active.dimension().equals(level.dimension().location().toString())
                && active.stall().equals(stall)) VISITS.remove(resident);
    }
}
