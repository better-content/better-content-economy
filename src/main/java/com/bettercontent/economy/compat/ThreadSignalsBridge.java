package com.bettercontent.economy.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Optional reflection bridge to the stable Better Content Threads integration API. */
public final class ThreadSignalsBridge {
    static final String API_CLASS = "com.bettercontent.threads.api.ThreadSignals";
    static final String ECONOMY_THREAD = "coins_do_not_climb";
    private static final Api API = resolve(API_CLASS);

    private ThreadSignalsBridge() {}

    public static void coinAcquired(
            final ServerPlayer player, final ResourceLocation denomination, final UUID itemEntityId) {
        emitCoinAcquired(API, player, denomination, pickupCorrelation(player.getUUID(), itemEntityId));
    }

    public static void authoredCoinSpent(final ServerPlayer player) {
        emitAuthoredCoinSpent(API, player);
    }

    static String pickupCorrelation(final UUID playerId, final UUID itemEntityId) {
        return playerId + ":coin:" + itemEntityId;
    }

    static void emitCoinAcquired(
            final Api api,
            final ServerPlayer player,
            final ResourceLocation denomination,
            final String correlation) {
        api.emit(player, "coin_acquired", denomination.toString(), correlation);
    }

    static void emitAuthoredCoinSpent(final Api api, final ServerPlayer player) {
        String correlation = api.activeCorrelation(player, ECONOMY_THREAD);
        if (correlation != null) api.emit(player, "authored_trade", "coin_spent", correlation);
    }

    static Api resolve(final String className) {
        try {
            Class<?> type = Class.forName(className);
            Method emit = type.getMethod(
                    "emit", ServerPlayer.class, String.class, String.class, String.class);
            Method activeCorrelation = type.getMethod("activeCorrelation", ServerPlayer.class, String.class);
            return new ReflectionApi(emit, activeCorrelation);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return NoOpApi.INSTANCE;
        }
    }

    interface Api {
        void emit(ServerPlayer player, String type, String value, String correlation);

        String activeCorrelation(ServerPlayer player, String threadId);
    }

    private record ReflectionApi(Method emitMethod, Method activeCorrelationMethod) implements Api {
        @Override
        public void emit(
                final ServerPlayer player,
                final String type,
                final String value,
                final String correlation) {
            try {
                emitMethod.invoke(null, player, type, value, correlation);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // Threads is optional; an unavailable or incompatible integration cannot break economy actions.
            }
        }

        @Override
        public String activeCorrelation(final ServerPlayer player, final String threadId) {
            try {
                return (String) activeCorrelationMethod.invoke(null, player, threadId);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                return null;
            }
        }
    }

    private enum NoOpApi implements Api {
        INSTANCE;

        @Override
        public void emit(
                final ServerPlayer player,
                final String type,
                final String value,
                final String correlation) {}

        @Override
        public String activeCorrelation(final ServerPlayer player, final String threadId) {
            return null;
        }
    }
}
