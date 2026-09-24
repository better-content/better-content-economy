package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SpiritDeliveryPlanTest {
    @Test
    void stacksAreDeterministicAndConserveEachCurrencyAmount() {
        Map<CurrencyIdentity, Integer> credits = Map.of(
                CurrencyIdentity.TEMPO, 65, CurrencyIdentity.IMPACT, 3);

        List<SpiritDeliveryPlan.StackGrant> first = SpiritDeliveryPlan.plan(credits, ignored -> 64);
        List<SpiritDeliveryPlan.StackGrant> second = SpiritDeliveryPlan.plan(credits, ignored -> 64);

        assertEquals(first, second);
        assertEquals(List.of(
                new SpiritDeliveryPlan.StackGrant(0, CurrencyIdentity.IMPACT, 3),
                new SpiritDeliveryPlan.StackGrant(1, CurrencyIdentity.TEMPO, 64),
                new SpiritDeliveryPlan.StackGrant(2, CurrencyIdentity.TEMPO, 1)), first);
        assertEquals(68, first.stream().mapToInt(SpiritDeliveryPlan.StackGrant::count).sum());
    }

    @Test
    void partialAcceptanceRetriesOnlyMissingStackIndexes() {
        List<SpiritDeliveryPlan.StackGrant> grants = SpiritDeliveryPlan.plan(
                Map.of(CurrencyIdentity.WORK, 129), ignored -> 64);
        assertFalse(SpiritDeliveryPlan.isComplete(grants, new HashSet<>(List.of(0, 2))));
        assertTrue(SpiritDeliveryPlan.isComplete(grants, new HashSet<>(List.of(0, 1, 2))));
        assertThrows(IllegalArgumentException.class,
                () -> SpiritDeliveryPlan.plan(Map.of(CurrencyIdentity.WORK, 1), ignored -> 0));
    }
}
