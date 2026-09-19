package com.bettercontent.economy.spirit;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Small transactional ledger for physical spirit releases.  A delivery is kept in-flight until
 * the caller acknowledges the actual entity creation, so a transient spawning failure never
 * spends the credit a second time.
 */
public final class SpiritCreditLedger {
    private final EnumMap<CurrencyIdentity, Integer> pending = empty();
    private final EnumMap<CurrencyIdentity, Integer> issued = empty();
    private UUID inFlightId;
    private EnumMap<CurrencyIdentity, Integer> inFlight;
    private long nextReleaseAt;

    public void credit(final Map<CurrencyIdentity, Integer> credits, final long dueAt) {
        merge(pending, credits);
        if (nextReleaseAt == 0 || dueAt < nextReleaseAt) nextReleaseAt = dueAt;
    }

    public Delivery beginDueDelivery(final long gameTime) {
        if (inFlight != null || pending.isEmpty() || gameTime < nextReleaseAt) return null;
        inFlightId = UUID.randomUUID();
        inFlight = copy(pending);
        pending.clear();
        return new Delivery(inFlightId, Map.copyOf(inFlight));
    }

    public Delivery retryDelivery() {
        return inFlight == null ? null : new Delivery(inFlightId, Map.copyOf(inFlight));
    }

    public void acknowledge(final UUID deliveryId, final long nextDueAt) {
        if (!deliveryId.equals(inFlightId)) return;
        merge(issued, inFlight);
        inFlightId = null;
        inFlight = null;
        nextReleaseAt = pending.isEmpty() ? 0 : nextDueAt;
    }

    /** Keeps the same durable delivery ID and amount for a later retry. */
    public void failDelivery(final UUID deliveryId) {
        if (!deliveryId.equals(inFlightId)) return;
    }

    public Map<CurrencyIdentity, Integer> pending() { return Map.copyOf(pending); }
    public Map<CurrencyIdentity, Integer> issued() { return Map.copyOf(issued); }
    public long nextReleaseAt() { return nextReleaseAt; }
    public UUID inFlightId() { return inFlightId; }
    public Map<CurrencyIdentity, Integer> inFlight() { return inFlight == null ? Map.of() : Map.copyOf(inFlight); }

    public void restore(final Map<CurrencyIdentity, Integer> restoredPending,
                        final Map<CurrencyIdentity, Integer> restoredIssued,
                        final UUID restoredInFlightId,
                        final Map<CurrencyIdentity, Integer> restoredInFlight,
                        final long restoredNextReleaseAt) {
        pending.clear(); pending.putAll(positive(restoredPending));
        issued.clear(); issued.putAll(positive(restoredIssued));
        inFlightId = restoredInFlightId;
        inFlight = restoredInFlightId == null ? null : copy(restoredInFlight);
        nextReleaseAt = restoredNextReleaseAt;
    }

    private static EnumMap<CurrencyIdentity, Integer> empty() { return new EnumMap<>(CurrencyIdentity.class); }
    private static EnumMap<CurrencyIdentity, Integer> copy(final Map<CurrencyIdentity, Integer> values) {
        return positive(values);
    }
    private static EnumMap<CurrencyIdentity, Integer> positive(final Map<CurrencyIdentity, Integer> values) {
        EnumMap<CurrencyIdentity, Integer> copy = empty();
        values.forEach((identity, count) -> { if (identity != null && count != null && count > 0) copy.put(identity, count); });
        return copy;
    }
    private static void merge(final EnumMap<CurrencyIdentity, Integer> target, final Map<CurrencyIdentity, Integer> credits) {
        credits.forEach((identity, count) -> {
            if (identity != null && count != null && count > 0) {
                target.merge(identity, count, Math::addExact);
            }
        });
    }

    public record Delivery(UUID id, Map<CurrencyIdentity, Integer> credits) {}
}
