package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
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

    @Test
    void acceptedDeliveryIsIdempotentAndCannotBeAcknowledgedTwice() {
        SpiritCreditLedger ledger = new SpiritCreditLedger();
        ledger.credit(Map.of(CurrencyIdentity.MOBILITY, 5), 10);
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(10);
        ledger.acknowledge(delivery.id(), 20);
        ledger.acknowledge(delivery.id(), 30);
        assertEquals(5, total(ledger.issued()));
        assertEquals(0, total(ledger.pending()) + total(ledger.inFlight()));
    }

    @Test
    void interruptionAfterReservationReloadsAsTheSameRetryReceipt() {
        SpiritCreditLedger ledger = new SpiritCreditLedger();
        ledger.credit(Map.of(CurrencyIdentity.ENDURANCE, 7), 50);
        SpiritCreditLedger.Delivery reserved = ledger.beginDueDelivery(50);
        SpiritCreditLedger restored = new SpiritCreditLedger();
        restored.restore(ledger.pending(), ledger.issued(), ledger.inFlightId(), ledger.inFlight(), ledger.nextReleaseAt());
        SpiritCreditLedger.Delivery retry = restored.retryDelivery();
        assertEquals(reserved.id(), retry.id());
        assertEquals(reserved.credits(), retry.credits());
        restored.acknowledge(retry.id(), 100);
        assertEquals(7, total(restored.issued()));
        assertNull(restored.retryDelivery());
    }

    @Test
    void pickedUpStackReceiptSurvivesSavedDataReloadUntilDeliveryAcknowledgement() {
        UUID player = UUID.randomUUID();
        SpiritCreditData data = new SpiritCreditData();
        SpiritCreditLedger ledger = data.ledger(player);
        ledger.credit(Map.of(CurrencyIdentity.WORK, 40), 50);
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(50);

        assertTrue(data.recordStackReceipt(delivery.id(), 3));
        assertFalse(data.recordStackReceipt(UUID.randomUUID(), 4));
        assertFalse(data.recordStackReceipt(delivery.id(), -1));

        SpiritCreditData restoredData = SpiritCreditData.load(data.save(new CompoundTag()));
        SpiritCreditLedger restored = restoredData.ledger(player);
        assertEquals(Set.of(3), restoredData.inFlightStackReceipts(delivery.id()));
        assertEquals(delivery.id(), restored.retryDelivery().id());
        restored.acknowledge(delivery.id(), 100);
        assertEquals(Set.of(), restoredData.inFlightStackReceipts(delivery.id()));
    }

    @Test
    void failedInsertionIsNotAutomaticallyRetriedAfterItsClaimWasSaved() {
        UUID player = UUID.randomUUID();
        SpiritCreditData data = new SpiritCreditData();
        SpiritCreditLedger ledger = data.ledger(player);
        ledger.credit(Map.of(CurrencyIdentity.WORK, 12), 50);
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(50);

        // Production records and saves this claim before calling the level insertion API.
        assertTrue(data.recordStackReceipt(delivery.id(), 0));
        SpiritCreditData restoredData = SpiritCreditData.load(data.save(new CompoundTag()));
        SpiritCreditLedger restored = restoredData.ledger(player);
        assertEquals(delivery.id(), restored.retryDelivery().id());
        assertEquals(Set.of(0), restoredData.inFlightStackReceipts(delivery.id()));

        // A false insertion result is ambiguous once the attempt boundary has been crossed.
        // A retry must skip it and finish the delivery without another insertion attempt.
        assertFalse(restoredData.recordStackReceipt(delivery.id(), 0));
        assertTrue(SpiritDeliveryPlan.isComplete(
                SpiritDeliveryPlan.plan(delivery.credits(), ignored -> 64),
                restoredData.inFlightStackReceipts(delivery.id())));
        restored.acknowledge(delivery.id(), 100);
        assertEquals(12, total(restored.issued()));
        assertNull(restored.retryDelivery());
    }

    @Test
    void ambiguousCrashAfterInsertionCannotIssueTheSameStackAgain() {
        UUID player = UUID.randomUUID();
        SpiritCreditData data = new SpiritCreditData();
        SpiritCreditLedger ledger = data.ledger(player);
        ledger.credit(Map.of(CurrencyIdentity.TEMPO, 5), 50);
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(50);

        // The durable claim precedes insertion. Simulate insertion succeeding followed by a
        // crash before delivery acknowledgement or a later pickup receipt.
        assertTrue(data.recordStackReceipt(delivery.id(), 0));
        SpiritCreditData restoredData = SpiritCreditData.load(data.save(new CompoundTag()));
        SpiritCreditLedger restored = restoredData.ledger(player);
        assertFalse(restoredData.recordStackReceipt(delivery.id(), 0));
        assertEquals(Set.of(0), restoredData.inFlightStackReceipts(delivery.id()));
        restored.acknowledge(delivery.id(), 100);
        assertEquals(5, total(restored.issued()));
        assertNull(restored.retryDelivery());
    }

    @Test
    void successfulStackReceiptAndAcknowledgementSurviveReloadExactlyOnce() {
        UUID player = UUID.randomUUID();
        SpiritCreditData data = new SpiritCreditData();
        SpiritCreditLedger ledger = data.ledger(player);
        ledger.credit(Map.of(CurrencyIdentity.IMPACT, 9), 50);
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(50);
        assertTrue(data.recordStackReceipt(delivery.id(), 0));
        ledger.acknowledge(delivery.id(), 100);

        SpiritCreditData restoredData = SpiritCreditData.load(data.save(new CompoundTag()));
        SpiritCreditLedger restored = restoredData.ledger(player);
        assertEquals(9, total(restored.issued()));
        assertEquals(0, total(restored.pending()) + total(restored.inFlight()));
        assertNull(restored.retryDelivery());
    }

    @Test
    void savedDataFileIsForcedAtomicallyAndWriteFailureRemainsDirty() throws IOException {
        SharedConstants.tryDetectVersion();
        Path root = Path.of(System.getProperty("user.home"), ".tmp", "spirit-credit-save-tests");
        Files.createDirectories(root);
        Path directory = Files.createDirectory(root.resolve(UUID.randomUUID().toString()));
        try {
            UUID player = UUID.randomUUID();
            SpiritCreditData data = new SpiritCreditData();
            SpiritCreditLedger ledger = data.ledger(player);
            ledger.credit(Map.of(CurrencyIdentity.IMPACT, 8), 50);
            SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(50);
            assertTrue(data.recordStackReceipt(delivery.id(), 0));
            data.setDirty();

            Path saveFile = directory.resolve("spirit-credits.dat");
            data.save(saveFile.toFile());
            assertFalse(data.isDirty());
            SpiritCreditData restored = SpiritCreditData.load(NbtIo.readCompressed(saveFile.toFile()).getCompound("data"));
            assertEquals(Set.of(0), restored.inFlightStackReceipts(delivery.id()));
            assertEquals(delivery.id(), restored.ledger(player).retryDelivery().id());

            Path blocker = directory.resolve("not-a-directory");
            Files.writeString(blocker, "occupied");
            data.setDirty();
            assertThrows(UncheckedIOException.class,
                    () -> data.save(blocker.resolve("spirit-credits.dat").toFile()));
            assertTrue(data.isDirty(), "a failed durable write must not clear the SavedData dirty flag");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void directorySyncPolicyAvoidsWindowsDirectoryHandlesButKeepsUnixSync() {
        assertFalse(SpiritCreditData.supportsParentDirectoryForce("Windows 11"));
        assertTrue(SpiritCreditData.supportsParentDirectoryForce("Linux"));
        assertTrue(SpiritCreditData.supportsParentDirectoryForce("Mac OS X"));
    }

    @Test
    void windowsReplacementUsesWriteThroughAndFailsClosedOnNativeFailure() {
        Path source = Path.of("receipt.tmp");
        Path target = Path.of("receipt.dat");
        int requiredFlags = com.sun.jna.platform.win32.WinBase.MOVEFILE_REPLACE_EXISTING
                | com.sun.jna.platform.win32.WinBase.MOVEFILE_WRITE_THROUGH;
        int[] observedFlags = {-1};

        assertDoesNotThrow(() -> SpiritCreditData.replaceSavedFile(source, target, "Windows 11",
                (from, to, flags) -> {
                    assertEquals(source.toString(), from);
                    assertEquals(target.toString(), to);
                    observedFlags[0] = flags;
                    return true;
                }));
        assertEquals(requiredFlags, observedFlags[0]);
        assertThrows(IOException.class, () -> SpiritCreditData.replaceSavedFile(source, target, "Windows 11",
                (from, to, flags) -> false));
    }

    @Test
    void quietWindowAccumulatesManyKillsIntoOneRegularDelivery() {
        SpiritCreditLedger ledger = new SpiritCreditLedger();
        for (int kill = 0; kill < 40; kill++) {
            ledger.credit(Map.of(CurrencyIdentity.WORK, 2, CurrencyIdentity.TEMPO, 1), 100);
        }
        assertNull(ledger.beginDueDelivery(99));
        SpiritCreditLedger.Delivery delivery = ledger.beginDueDelivery(100);
        assertNotNull(delivery);
        assertEquals(120, total(delivery.credits()));
        ledger.acknowledge(delivery.id(), 200);
        assertEquals(120, total(ledger.issued()));
        assertEquals(0, total(ledger.pending()) + total(ledger.inFlight()));
    }

    private static int total(Map<CurrencyIdentity, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }
}
