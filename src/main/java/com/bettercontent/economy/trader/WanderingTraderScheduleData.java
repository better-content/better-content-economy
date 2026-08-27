package com.bettercontent.economy.trader;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class WanderingTraderScheduleData extends SavedData {
    private static final String DATA_NAME = "better_content_economy_wandering_trader";
    private static final int SCHEMA_VERSION = 1;

    private boolean initialized;
    private long nextAttemptGameTime;
    private int nextThemeIndex;
    private UUID activeTraderId;

    public static WanderingTraderScheduleData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WanderingTraderScheduleData::load,
                WanderingTraderScheduleData::new,
                DATA_NAME);
    }

    public static WanderingTraderScheduleData load(final CompoundTag tag) {
        final WanderingTraderScheduleData data = new WanderingTraderScheduleData();
        data.initialized = tag.getBoolean("Initialized");
        data.nextAttemptGameTime = tag.getLong("NextAttemptGameTime");
        data.nextThemeIndex = tag.getInt("NextThemeIndex");
        if (tag.hasUUID("ActiveTrader")) {
            data.activeTraderId = tag.getUUID("ActiveTrader");
        }
        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        tag.putBoolean("Initialized", initialized);
        tag.putLong("NextAttemptGameTime", nextAttemptGameTime);
        tag.putInt("NextThemeIndex", nextThemeIndex);
        if (activeTraderId != null) {
            tag.putUUID("ActiveTrader", activeTraderId);
        }
        return tag;
    }

    public void initializeIfNeeded(final long gameTime, final int initialDelay) {
        if (initialized) {
            return;
        }
        initialized = true;
        nextAttemptGameTime = gameTime + initialDelay;
        setDirty();
    }

    public boolean isVisitDue(final long gameTime) {
        return initialized && gameTime >= nextAttemptGameTime;
    }

    public WanderingTraderTheme nextTheme() {
        return WanderingTraderTheme.fromIndex(nextThemeIndex);
    }

    public UUID activeTraderId() {
        return activeTraderId;
    }

    public long nextAttemptGameTime() {
        return nextAttemptGameTime;
    }

    public void scheduleRetry(final long gameTime, final int retryDelay) {
        nextAttemptGameTime = gameTime + retryDelay;
        setDirty();
    }

    public void completeVisit(final long gameTime, final int visitInterval, final UUID traderId) {
        activeTraderId = traderId;
        nextAttemptGameTime = gameTime + visitInterval;
        nextThemeIndex = Math.floorMod(nextThemeIndex + 1, WanderingTraderTheme.values().length);
        setDirty();
    }

    public void clearActiveTrader() {
        if (activeTraderId != null) {
            activeTraderId = null;
            setDirty();
        }
    }
}
