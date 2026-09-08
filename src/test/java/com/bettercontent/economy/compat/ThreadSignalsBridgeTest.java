package com.bettercontent.economy.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ThreadSignalsBridgeTest {
    @BeforeEach
    void resetFakeApi() {
        FakeSignals.active = null;
        FakeSignals.type = null;
        FakeSignals.value = null;
        FakeSignals.correlation = null;
    }

    @Test
    void resolvesThePublicFourArgumentSignalAndCorrelationSurface() {
        ThreadSignalsBridge.Api api = ThreadSignalsBridge.resolve(FakeSignals.class.getName());
        FakeSignals.active = "one-episode";

        assertEquals("one-episode", api.activeCorrelation(null, "coins_do_not_climb"));
        api.emit(null, "authored_trade", "coin_spent", "one-episode");

        assertEquals("authored_trade", FakeSignals.type);
        assertEquals("coin_spent", FakeSignals.value);
        assertEquals("one-episode", FakeSignals.correlation);
    }

    @Test
    void acquisitionAndTradeReuseOneBoundedEpisodeToken() {
        ThreadSignalsBridge.Api api = ThreadSignalsBridge.resolve(FakeSignals.class.getName());
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID entity = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String token = ThreadSignalsBridge.pickupCorrelation(player, entity);

        ThreadSignalsBridge.emitCoinAcquired(
                api, null, ResourceLocation.fromNamespaceAndPath("createdeco", "copper_coin"), token);
        assertEquals("coin_acquired", FakeSignals.type);
        assertEquals("createdeco:copper_coin", FakeSignals.value);
        assertEquals(token, FakeSignals.active);

        ThreadSignalsBridge.emitAuthoredCoinSpent(api, null);
        assertEquals("authored_trade", FakeSignals.type);
        assertEquals("coin_spent", FakeSignals.value);
        assertEquals(token, FakeSignals.correlation);
        assertTrue(token.length() <= 128);
    }

    @Test
    void missingOptionalApiIsAQuietNoOp() {
        ThreadSignalsBridge.Api api = ThreadSignalsBridge.resolve("missing.optional.ThreadSignals");
        api.emit(null, "coin_acquired", "createdeco:copper_coin", "episode");
        assertNull(api.activeCorrelation(null, "coins_do_not_climb"));
    }

    public static final class FakeSignals {
        static String active;
        static String type;
        static String value;
        static String correlation;

        private FakeSignals() {}

        public static void emit(
                final ServerPlayer player,
                final String emittedType,
                final String emittedValue,
                final String emittedCorrelation) {
            type = emittedType;
            value = emittedValue;
            correlation = emittedCorrelation;
            if ("coin_acquired".equals(emittedType)) active = emittedCorrelation;
        }

        public static String activeCorrelation(final ServerPlayer player, final String threadId) {
            return active;
        }
    }
}
