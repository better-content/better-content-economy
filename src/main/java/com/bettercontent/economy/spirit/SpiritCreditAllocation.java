package com.bettercontent.economy.spirit;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Maps Malum's ordinary drops to the eight economy identities without changing total value. */
public final class SpiritCreditAllocation {
    private SpiritCreditAllocation() {}

    public static Map<CurrencyIdentity, Integer> fromNative(final List<ItemStack> nativeDrops,
                                                             final UUID victimId,
                                                             final UUID recipientId) {
        return fromNative(nativeDrops, victimId, recipientId, 0L);
    }

    /** Allocates the same number of credits while biasing composition by kill region. */
    public static Map<CurrencyIdentity, Integer> fromNative(final List<ItemStack> nativeDrops,
                                                             final UUID victimId,
                                                             final UUID recipientId,
                                                             final long regionSeed) {
        java.util.ArrayList<CurrencyIdentity> units = new java.util.ArrayList<>();
        for (ItemStack stack : nativeDrops) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            CurrencyIdentity mapped = id == null ? null : CurrencyIdentity.fromLegacyNativeSpirit(id);
            if (mapped != null) for (int unit = 0; unit < stack.getCount(); unit++) units.add(mapped);
        }
        return fromUnits(units, victimId, recipientId, regionSeed);
    }

    static Map<CurrencyIdentity, Integer> fromUnits(final List<CurrencyIdentity> units,
                                                     final UUID victimId,
                                                     final UUID recipientId,
                                                     final long regionSeed) {
        EnumMap<CurrencyIdentity, Integer> result = new EnumMap<>(CurrencyIdentity.class);
        long state = victimId.getMostSignificantBits() ^ victimId.getLeastSignificantBits()
                ^ recipientId.getMostSignificantBits() ^ recipientId.getLeastSignificantBits() ^ regionSeed;
        for (CurrencyIdentity mapped : units) {
                state = mix(state + 0x9E3779B97F4A7C15L);
                // One eighth remains a global timing currency; the rest follows the native
                // aspect with a region dependent adjacent shift for visible scarcity.
                CurrencyIdentity identity = Math.floorMod(state, 8) == 0
                        ? CurrencyIdentity.TEMPO : regional(mapped, state);
                result.merge(identity, 1, Integer::sum);
        }
        return Map.copyOf(result);
    }

    private static CurrencyIdentity regional(final CurrencyIdentity mapped, final long state) {
        CurrencyIdentity[] values = CurrencyIdentity.values();
        int shift = Math.floorMod((int) (state >>> 32), 3) - 1;
        return values[Math.floorMod(mapped.ordinal() + shift, values.length)];
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
