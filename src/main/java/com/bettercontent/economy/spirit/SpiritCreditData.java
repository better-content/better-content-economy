package com.bettercontent.economy.spirit;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world durable pending credit state. */
public final class SpiritCreditData extends SavedData {
    private static final String DATA_NAME = "better_content_economy_spirit_credits";
    private final Map<UUID, SpiritCreditLedger> ledgers = new java.util.HashMap<>();

    public static SpiritCreditData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SpiritCreditData::load, SpiritCreditData::new, DATA_NAME);
    }

    public SpiritCreditLedger ledger(final UUID playerId) {
        return ledgers.computeIfAbsent(playerId, ignored -> new SpiritCreditLedger());
    }

    public boolean recordStackReceipt(final UUID deliveryId, final int stackIndex) {
        return ledgers.values().stream().anyMatch(ledger -> ledger.recordStackReceipt(deliveryId, stackIndex));
    }

    public Set<Integer> inFlightStackReceipts(final UUID deliveryId) {
        return ledgers.values().stream()
                .filter(ledger -> deliveryId.equals(ledger.inFlightId()))
                .findFirst()
                .map(ledger -> ledger.inFlightStackReceipts(deliveryId))
                .orElseGet(Set::of);
    }

    public static SpiritCreditData load(final CompoundTag tag) {
        SpiritCreditData data = new SpiritCreditData();
        for (Tag entryTag : tag.getList("Ledgers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (!entry.hasUUID("Player")) continue;
            SpiritCreditLedger ledger = new SpiritCreditLedger();
            UUID inFlight = entry.hasUUID("InFlightId") ? entry.getUUID("InFlightId") : null;
            ledger.restore(read(entry.getCompound("Pending")), read(entry.getCompound("Issued")), inFlight,
                    read(entry.getCompound("InFlight")), Arrays.stream(entry.getIntArray("InFlightStackReceipts"))
                            .boxed().collect(java.util.stream.Collectors.toSet()), entry.getLong("NextReleaseAt"));
            data.ledgers.put(entry.getUUID("Player"), ledger);
        }
        return data;
    }

    @Override public CompoundTag save(final CompoundTag tag) {
        ListTag entries = new ListTag();
        ledgers.forEach((player, ledger) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.put("Pending", write(ledger.pending()));
            entry.put("Issued", write(ledger.issued()));
            if (ledger.inFlightId() != null) entry.putUUID("InFlightId", ledger.inFlightId());
            entry.put("InFlight", write(ledger.inFlight()));
            entry.putIntArray("InFlightStackReceipts", ledger.inFlightId() == null ? new int[0]
                    : ledger.inFlightStackReceipts(ledger.inFlightId()).stream().mapToInt(Integer::intValue).toArray());
            entry.putLong("NextReleaseAt", ledger.nextReleaseAt());
            entries.add(entry);
        });
        tag.put("Ledgers", entries);
        return tag;
    }

    /**
     * SavedData's default file writer logs IO failures and clears the dirty flag anyway. Delivery
     * receipts are an at-most-once boundary, so write this data through a forced sibling file and
     * atomic replacement, and propagate every failure before issuance can continue.
     */
    @Override public void save(final File file) {
        if (!isDirty()) return;
        Path target = file.toPath().toAbsolutePath();
        Path parent = target.getParent();
        if (parent == null) throw new UncheckedIOException(new IOException("Saved data file has no parent"));
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            CompoundTag root = new CompoundTag();
            root.put("data", save(new CompoundTag()));
            NbtUtils.addCurrentDataVersion(root);
            try (FileOutputStream stream = new FileOutputStream(temporary.toFile())) {
                // NbtIo closes its argument, so keep the descriptor open for fd.sync().
                NbtIo.writeCompressed(root, new FilterOutputStream(stream) {
                    @Override public void close() throws IOException { flush(); }
                });
                stream.getFD().sync();
            }
            String osName = System.getProperty("os.name", "");
            replaceSavedFile(temporary, target, osName, SpiritCreditData::windowsMoveFileEx);
            // Re-force the moved file on every platform. Windows' Java 17 provider opens
            // FileChannels with FILE_ATTRIBUTE_NORMAL but without FILE_FLAG_BACKUP_SEMANTICS,
            // which Windows requires for directory handles. Skip only that unsupported step;
            // keep it on Unix-like systems where read-only directory descriptors are available.
            try (FileChannel savedFile = FileChannel.open(target, StandardOpenOption.WRITE)) {
                savedFile.force(true);
            }
            if (supportsParentDirectoryForce(osName)) {
                try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                    directory.force(true);
                }
            }
            setDirty(false);
        } catch (IOException exception) {
            setDirty(true);
            throw new UncheckedIOException("Could not durably save spirit credit data", exception);
        }
    }

    static boolean supportsParentDirectoryForce(final String osName) {
        return !osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    static void replaceSavedFile(final Path temporary, final Path target, final String osName,
                                 final WindowsFileMover windowsMover) throws IOException {
        if (osName.toLowerCase(Locale.ROOT).startsWith("windows")) {
            int flags = WinBase.MOVEFILE_REPLACE_EXISTING | WinBase.MOVEFILE_WRITE_THROUGH;
            if (!windowsMover.move(temporary.toString(), target.toString(), flags)) {
                throw new IOException("MoveFileEx with MOVEFILE_WRITE_THROUGH failed, Windows error "
                        + Native.getLastError());
            }
        } else {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean windowsMoveFileEx(final String source, final String target, final int flags) {
        return Kernel32.INSTANCE.MoveFileEx(source, target, new WinDef.DWORD(flags));
    }

    @FunctionalInterface
    interface WindowsFileMover {
        boolean move(String source, String target, int flags);
    }

    private static CompoundTag write(final Map<CurrencyIdentity, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((identity, count) -> tag.putInt(identity.id(), count));
        return tag;
    }
    private static Map<CurrencyIdentity, Integer> read(final CompoundTag tag) {
        Map<CurrencyIdentity, Integer> values = new EnumMap<>(CurrencyIdentity.class);
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            int count = tag.getInt(identity.id());
            if (count > 0) values.put(identity, count);
        }
        return values;
    }
}
