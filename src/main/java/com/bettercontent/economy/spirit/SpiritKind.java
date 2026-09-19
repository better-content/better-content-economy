package com.bettercontent.economy.spirit;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** The seven ordinary Malum spirits that participate in village commerce. */
public enum SpiritKind {
    SACRED,
    WICKED,
    ARCANE,
    AERIAL,
    AQUEOUS,
    EARTHEN,
    INFERNAL;

    private static final SpiritKind[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation itemId() {
        return new ResourceLocation("malum", id() + "_spirit");
    }

    public static SpiritKind fromId(final String id) {
        if (id == null) return null;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.endsWith("_spirit")) path = path.substring(0, path.length() - 7);
        for (SpiritKind kind : VALUES) if (kind.id().equals(path)) return kind;
        return null;
    }

    public static SpiritKind fromIndex(final int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}
