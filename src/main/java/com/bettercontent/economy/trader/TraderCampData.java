package com.bettercontent.economy.trader;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Durable target state lets a moved post retarget a trader whose old chunk is unloaded. */
public final class TraderCampData extends SavedData {
    private static final String DATA_NAME = "better_content_economy_trader_camp";
    @Nullable private UUID traderId;
    private String dimension = "minecraft:overworld";
    private long targetPos;
    private String facing = Direction.NORTH.getName();

    public static TraderCampData get(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                TraderCampData::load, TraderCampData::new, DATA_NAME);
    }

    public static TraderCampData load(final CompoundTag tag) {
        TraderCampData data = new TraderCampData();
        if (tag.hasUUID("Trader")) data.traderId = tag.getUUID("Trader");
        if (tag.contains("Dimension")) data.dimension = tag.getString("Dimension");
        data.targetPos = tag.getLong("TargetPos");
        if (tag.contains("Facing")) data.facing = tag.getString("Facing");
        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        if (traderId != null) tag.putUUID("Trader", traderId);
        tag.putString("Dimension", dimension);
        tag.putLong("TargetPos", targetPos);
        tag.putString("Facing", facing);
        return tag;
    }

    public void setTarget(final UUID trader, final String dimension, final BlockPos pos, final Direction facing) {
        traderId = trader;
        this.dimension = dimension;
        targetPos = pos.asLong();
        this.facing = facing.getName();
        setDirty();
    }

    public boolean belongsTo(final UUID trader) { return trader.equals(traderId); }
    public String dimension() { return dimension; }
    public BlockPos targetPos() { return BlockPos.of(targetPos); }

    public Direction facing() {
        Direction parsed = Direction.byName(facing);
        return parsed != null && parsed.getAxis().isHorizontal() ? parsed : Direction.NORTH;
    }

    public void clear(final UUID trader) {
        if (belongsTo(trader)) {
            traderId = null;
            setDirty();
        }
    }
}
