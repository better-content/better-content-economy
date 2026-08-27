package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class WanderingTraderScheduleDataTest {
    @Test
    void initializesAtDayTwoAndRepeatsFiveDaysAfterSuccess() {
        final WanderingTraderScheduleData schedule = new WanderingTraderScheduleData();
        schedule.initializeIfNeeded(100L, 48_000);

        assertFalse(schedule.isVisitDue(48_099L));
        assertTrue(schedule.isVisitDue(48_100L));
        assertEquals(WanderingTraderTheme.NATURALIST, schedule.nextTheme());

        final UUID traderId = UUID.fromString("00000000-0000-0000-0000-000000000019");
        schedule.completeVisit(48_100L, 120_000, traderId);
        assertEquals(168_100L, schedule.nextAttemptGameTime());
        assertEquals(traderId, schedule.activeTraderId());
        assertEquals(WanderingTraderTheme.SURVEYOR, schedule.nextTheme());
    }

    @Test
    void retryDoesNotAdvanceThemeAndSavedStateRoundTrips() {
        final WanderingTraderScheduleData schedule = new WanderingTraderScheduleData();
        schedule.initializeIfNeeded(0L, 48_000);
        schedule.scheduleRetry(48_000L, 1_200);

        final WanderingTraderScheduleData loaded = WanderingTraderScheduleData.load(schedule.save(new CompoundTag()));
        assertEquals(49_200L, loaded.nextAttemptGameTime());
        assertEquals(WanderingTraderTheme.NATURALIST, loaded.nextTheme());
        assertNull(loaded.activeTraderId());
    }

    @Test
    void themeCycleIsStable() {
        assertEquals(WanderingTraderTheme.SURVEYOR, WanderingTraderTheme.NATURALIST.next());
        assertEquals(WanderingTraderTheme.QUARTERMASTER, WanderingTraderTheme.SURVEYOR.next());
        assertEquals(WanderingTraderTheme.ANTIQUARIAN, WanderingTraderTheme.QUARTERMASTER.next());
        assertEquals(WanderingTraderTheme.NATURALIST, WanderingTraderTheme.ANTIQUARIAN.next());
    }
}
