package com.bettercontent.economy.spirit;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Commerce themes. Tempo is the eighth economy identity and has no legacy Malum item. */
public enum SpiritKind {
    SACRED,
    WICKED,
    ARCANE,
    AERIAL,
    AQUEOUS,
    EARTHEN,
    INFERNAL,
    TEMPO;

    private static final SpiritKind[] VALUES = values();
    private static final SpiritKind[] NATIVE_VALUES = {SACRED, WICKED, ARCANE, AERIAL, AQUEOUS, EARTHEN, INFERNAL};

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation itemId() {
        if (this == TEMPO) return CurrencyIdentity.TEMPO.itemId();
        return new ResourceLocation("malum", id() + "_spirit");
    }

    public CurrencyIdentity currencyIdentity() {
        return this == TEMPO ? CurrencyIdentity.TEMPO : CurrencyIdentity.fromLegacyNativeSpirit(itemId());
    }

    public static SpiritKind fromId(final String id) {
        if (id == null) return null;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.endsWith("_spirit")) path = path.substring(0, path.length() - 7);
        for (SpiritKind kind : VALUES) if (kind.id().equals(path)) return kind;
        return null;
    }

    public static SpiritKind fromIndex(final int index) {
        return NATIVE_VALUES[Math.floorMod(index, NATIVE_VALUES.length)];
    }
}
