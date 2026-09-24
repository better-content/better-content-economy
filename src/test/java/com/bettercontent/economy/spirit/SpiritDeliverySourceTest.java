package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SpiritDeliverySourceTest {
    @Test
    void windowsNativeReplacementDependenciesArePinnedAndJarJarPackaged() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(build.contains("implementation(jarJar(\"net.java.dev.jna:jna:[5.14.0,5.14.0]\")!!)"));
        assertTrue(build.contains("implementation(jarJar(\"net.java.dev.jna:jna-platform:[5.14.0,5.14.0]\")!!)"));
    }

    @Test
    void eachStackIsPersistentlyClaimedBeforeInsertionAndRejectedInsertionIsNotRetried() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/spirit/SpiritAcquisition.java"));
        assertTrue(source.contains("DELIVERY_STACK_TAG"));
        assertTrue(source.contains("receipts.stackIndexes().contains(stack.grant().index())"));
        int claimBoundary = source.indexOf("// Claim and synchronously save this exact stack");
        int receipt = source.indexOf("data.recordStackReceipt(delivery.id(), stackIndex)", claimBoundary);
        int dirty = source.indexOf("data.setDirty()", receipt);
        int save = source.indexOf("overworld.getDataStorage().save()", dirty);
        int insertion = source.indexOf("spawnCurrencyStack(player, stack.stack())", save);
        assertTrue(claimBoundary >= 0 && receipt > claimBoundary && dirty > receipt
                        && save > dirty && insertion > save,
                "the durable per-stack claim must complete before the potentially ambiguous insertion");
        assertTrue(source.contains("return level.addFreshEntity(entity)"));
        assertTrue(source.contains("receipts.legacyReceipt()"));
        assertTrue(source.contains("its durable claim prevents retry"));
    }

    @Test
    void successfulPickupPersistsThePerStackReceiptForCustodyOutsideLoadedEntities() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/spirit/SpiritAcquisition.java"));
        String savedData = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/spirit/SpiritCreditData.java"));
        assertTrue(source.contains("onCurrencyPickup(final PlayerEvent.ItemPickupEvent event)"));
        assertTrue(source.contains("data.recordStackReceipt(deliveryId, stackIndex)"));
        assertTrue(source.contains("overworld.getDataStorage().save()"));
        assertTrue(source.contains("receipts.stackIndexes().addAll(data.inFlightStackReceipts(delivery.id()))"));
        assertTrue(savedData.contains("InFlightStackReceipts"));
        assertTrue(savedData.contains("public Set<Integer> inFlightStackReceipts(final UUID deliveryId)"));
    }

    @Test
    void spiritSavedDataUsesForcedAtomicWritesAndPropagatesIoFailures() throws IOException {
        String savedData = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/spirit/SpiritCreditData.java"));
        assertTrue(savedData.contains("@Override public void save(final File file)"));
        assertTrue(savedData.contains("stream.getFD().sync()"));
        assertTrue(savedData.contains("StandardCopyOption.ATOMIC_MOVE"));
        assertTrue(savedData.contains("WinBase.MOVEFILE_REPLACE_EXISTING | WinBase.MOVEFILE_WRITE_THROUGH"));
        assertTrue(savedData.contains("windowsMover.move(temporary.toString(), target.toString(), flags)"));
        assertTrue(savedData.contains("savedFile.force(true)"));
        assertTrue(savedData.contains("directory.force(true)"));
        assertTrue(savedData.contains("if (supportsParentDirectoryForce(osName))"));
        assertTrue(savedData.contains("throw new UncheckedIOException(\"Could not durably save spirit credit data\""));
        assertTrue(savedData.indexOf("replaceSavedFile(temporary, target, osName") < savedData.indexOf("setDirty(false)"));
    }
}
