package com.bettercontent.economy.spirit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/** Deterministic per-stack manifest so partial entity acceptance can be retried idempotently. */
public final class SpiritDeliveryPlan {
    private SpiritDeliveryPlan() {}

    public static List<StackGrant> plan(final Map<CurrencyIdentity, Integer> credits,
                                        final ToIntFunction<CurrencyIdentity> maxStackSize) {
        final List<StackGrant> grants = new ArrayList<>();
        int index = 0;
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            int remaining = Math.max(0, credits.getOrDefault(identity, 0));
            if (remaining == 0) continue;
            int stackSize = maxStackSize.applyAsInt(identity);
            if (stackSize <= 0) throw new IllegalArgumentException("Currency stack size must be positive");
            while (remaining > 0) {
                int count = Math.min(remaining, stackSize);
                grants.add(new StackGrant(index++, identity, count));
                remaining -= count;
            }
        }
        return List.copyOf(grants);
    }

    public static boolean isComplete(final List<StackGrant> grants, final Set<Integer> acceptedStackIndexes) {
        for (StackGrant grant : grants) {
            if (!acceptedStackIndexes.contains(grant.index())) return false;
        }
        return true;
    }

    public record StackGrant(int index, CurrencyIdentity identity, int count) {}
}
