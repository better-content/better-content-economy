package com.bettercontent.spiritcommerce.resident;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Remembers hand-placed terrain so resource gathering cannot dismantle a build. */
public final class ResidentTerrainData extends SavedData {
    private final Set<Long> placed = new HashSet<>();
    private boolean saturated;
    private ResidentTerrainData() {}

    public static ResidentTerrainData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ResidentTerrainData::load, ResidentTerrainData::new,
                "better_spirit_commerce_player_terrain");
    }

    private static ResidentTerrainData load(CompoundTag tag) {
        ResidentTerrainData data = new ResidentTerrainData();
        for (long pos : tag.getLongArray("Placed")) data.placed.add(pos);
        data.saturated = tag.getBoolean("Saturated");
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Placed", placed.stream().mapToLong(Long::longValue).toArray());
        tag.putBoolean("Saturated", saturated);
        return tag;
    }

    public boolean protectedAt(BlockPos pos) { return saturated || placed.contains(pos.asLong()); }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Player)
                || !resource(event.getPlacedBlock())) return;
        ResidentTerrainData data = get(level);
        if (data.placed.size() >= 1_000_000) data.saturated = true;
        else data.placed.add(event.getPos().asLong());
        data.setDirty();
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof Player)) return;
        ResidentTerrainData data = get(level);
        if (data.placed.remove(event.getPos().asLong())) data.setDirty();
    }

    private static boolean resource(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(Blocks.STONE) || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK) || state.getBlock().getDescriptionId().contains("regolith");
    }
}
