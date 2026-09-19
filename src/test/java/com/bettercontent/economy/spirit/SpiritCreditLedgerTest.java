package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class SpiritCreditLedgerTest {
    @Test
    void pendingAndIssuedValueAreConservedAcrossARelease() {
        SpiritCreditLedger ledger = new SpiritCreditLedger();
        ledger.credit(Map.of(CurrencyIdentity.IMPACT, 3, CurrencyIdentity.TEMPO, 1), 100);

        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(100);
        assertNotNull(delivery);
        assertEquals(4, total(ledger.pending()) + total(ledger.inFlight()) + total(ledger.issued()));
        ledger.acknowledge(delivery.id(), 200);

        assertEquals(4, total(ledger.pending()) + total(ledger.inFlight()) + total(ledger.issued()));
        assertEquals(4, total(ledger.issued()));
    }

    @Test
    void retryReusesTheSameInFlightDeliveryWithoutSpendingAgain() {
        SpiritCreditLedger ledger = new SpiritCreditLedger();
        ledger.credit(Map.of(CurrencyIdentity.WORK, 2), 5);
        SpiritCreditLedger.Delivery first = ledger.beginDueDelivery(5);
        ledger.failDelivery(first.id());
        SpiritCreditLedger.Delivery retry = ledger.retryDelivery();

        assertEquals(first.id(), retry.id());
        assertEquals(first.credits(), retry.credits());
        assertEquals(2, total(ledger.inFlight()));
        ledger.acknowledge(retry.id(), 105);
        assertNull(ledger.retryDelivery());
        assertEquals(2, total(ledger.issued()));
    }

    @Test
    void restoredInFlightAndPendingCreditsKeepTheirExactAccounting() {
        SpiritCreditLedger original = new SpiritCreditLedger();
        original.credit(Map.of(CurrencyIdentity.RENEWAL, 2), 40);
        SpiritCreditLedger.Delivery inFlight = original.beginDueDelivery(40);
        original.credit(Map.of(CurrencyIdentity.CONTROL, 3), 140);

        SpiritCreditLedger restored = new SpiritCreditLedger();
        restored.restore(original.pending(), original.issued(), original.inFlightId(), original.inFlight(), original.nextReleaseAt());
        assertEquals(inFlight.id(), restored.retryDelivery().id());
        assertEquals(5, total(restored.pending()) + total(restored.inFlight()) + total(restored.issued()));
    }

    @Test
    void savedDataReloadPreservesPendingAndRetryableValue() {
        UUID player = UUID.randomUUID();
        SpiritCreditData data = new SpiritCreditData();
        SpiritCreditLedger ledger = data.ledger(player);
        ledger.credit(Map.of(CurrencyIdentity.IMPACT, 4), 20);
        ledger.beginDueDelivery(20);
        ledger.credit(Map.of(CurrencyIdentity.TEMPO, 2), 120);

        SpiritCreditData reloaded = SpiritCreditData.load(data.save(new CompoundTag()));
        SpiritCreditLedger restored = reloaded.ledger(player);
        assertEquals(6, total(restored.pending()) + total(restored.inFlight()) + total(restored.issued()));
        assertNotNull(restored.retryDelivery());
        assertEquals(2, total(restored.pending()));
    }

    private static int total(Map<CurrencyIdentity, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }
}
