package com.bettercontent.spiritcommerce.resident;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Records which fresh-world village sites have received starter workshops. */
public final class SettlementData extends SavedData {
    private final Set<Long> sites = new HashSet<>();

    public static SettlementData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SettlementData::load, SettlementData::new,
                "better_spirit_commerce_settlements");
    }

    public static SettlementData load(CompoundTag tag) {
        SettlementData data = new SettlementData();
        for (long site : tag.getLongArray("Sites")) data.sites.add(site);
        return data;
    }

    public boolean mark(long site) {
        if (!sites.add(site)) return false;
        setDirty();
        return true;
    }

    public boolean contains(long site) { return sites.contains(site); }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Sites", sites.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
