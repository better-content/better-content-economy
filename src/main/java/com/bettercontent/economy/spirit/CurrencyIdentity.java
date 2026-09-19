package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Eight economy-owned currencies. Native Malum spirits remain crafting reagents. */
public enum CurrencyIdentity {
    IMPACT("malum:infernal_spirit"),
    TEMPO(null),
    WORK("malum:arcane_spirit"),
    MOBILITY("malum:aerial_spirit"),
    ENDURANCE("malum:aqueous_spirit"),
    ROBUSTNESS("malum:earthen_spirit"),
    RENEWAL("malum:sacred_spirit"),
    CONTROL("malum:wicked_spirit");

    private final ResourceLocation legacyNativeSpirit;

    CurrencyIdentity(final String legacyNativeSpirit) {
        this.legacyNativeSpirit = legacyNativeSpirit == null ? null : new ResourceLocation(legacyNativeSpirit);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation itemId() {
        return new ResourceLocation(BetterContentEconomy.MOD_ID, id() + "_spirit");
    }

    public ResourceLocation legacyNativeSpirit() {
        return legacyNativeSpirit;
    }

    public static CurrencyIdentity fromLegacyNativeSpirit(final ResourceLocation id) {
        if (id == null) return null;
        for (CurrencyIdentity identity : values()) {
            if (id.equals(identity.legacyNativeSpirit)) return identity;
        }
        return null;
    }
}
