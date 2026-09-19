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
        EnumMap<CurrencyIdentity, Integer> result = new EnumMap<>(CurrencyIdentity.class);
        long state = victimId.getMostSignificantBits() ^ victimId.getLeastSignificantBits()
                ^ recipientId.getMostSignificantBits() ^ recipientId.getLeastSignificantBits();
        for (ItemStack stack : nativeDrops) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            CurrencyIdentity mapped = id == null ? null : CurrencyIdentity.fromLegacyNativeSpirit(id);
            if (mapped == null) continue;
            for (int unit = 0; unit < stack.getCount(); unit++) {
                state = mix(state + 0x9E3779B97F4A7C15L);
                CurrencyIdentity identity = Math.floorMod(state, 8) == 0 ? CurrencyIdentity.TEMPO : mapped;
                result.merge(identity, 1, Integer::sum);
            }
        }
        return Map.copyOf(result);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
