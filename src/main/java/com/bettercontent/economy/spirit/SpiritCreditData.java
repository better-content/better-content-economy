package com.bettercontent.economy.spirit;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

    public static SpiritCreditData load(final CompoundTag tag) {
        SpiritCreditData data = new SpiritCreditData();
        for (Tag entryTag : tag.getList("Ledgers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) entryTag;
            if (!entry.hasUUID("Player")) continue;
            SpiritCreditLedger ledger = new SpiritCreditLedger();
            UUID inFlight = entry.hasUUID("InFlightId") ? entry.getUUID("InFlightId") : null;
            ledger.restore(read(entry.getCompound("Pending")), read(entry.getCompound("Issued")), inFlight,
                    read(entry.getCompound("InFlight")), entry.getLong("NextReleaseAt"));
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
            entry.putLong("NextReleaseAt", ledger.nextReleaseAt());
            entries.add(entry);
        });
        tag.put("Ledgers", entries);
        return tag;
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
