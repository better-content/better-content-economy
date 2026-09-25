package com.bettercontent.economy.spirit;

import com.bettercontent.economy.mixin.MalumSpiritTypeAccessor;
import com.bettercontent.economy.registry.CurrencyItems;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import com.sammy.malum.core.systems.spirit.SpiritVisualMotif;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import team.lodestar.lodestone.systems.easing.Easing;

/** Malum visual types and reagent identities for the physical economy spirits. */
public final class EconomySpiritTypes {
    private static final Map<CurrencyIdentity, MalumSpiritType> TYPES = new EnumMap<>(CurrencyIdentity.class);
    private static boolean initialized;

    private EconomySpiritTypes() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        TYPES.put(CurrencyIdentity.IMPACT, SpiritTypeRegistry.INFERNAL_SPIRIT);
        TYPES.put(CurrencyIdentity.WORK, SpiritTypeRegistry.ARCANE_SPIRIT);
        TYPES.put(CurrencyIdentity.MOBILITY, SpiritTypeRegistry.AERIAL_SPIRIT);
        TYPES.put(CurrencyIdentity.ENDURANCE, SpiritTypeRegistry.AQUEOUS_SPIRIT);
        TYPES.put(CurrencyIdentity.ROBUSTNESS, SpiritTypeRegistry.EARTHEN_SPIRIT);
        TYPES.put(CurrencyIdentity.RENEWAL, SpiritTypeRegistry.SACRED_SPIRIT);
        TYPES.put(CurrencyIdentity.CONTROL, SpiritTypeRegistry.WICKED_SPIRIT);
        Color gold = new Color(0xD8BE62);
        Color paleGold = new Color(0xFFE9A8);
        TYPES.put(CurrencyIdentity.TEMPO, SpiritTypeRegistry.register(MalumSpiritType.create("tempo",
                new SpiritVisualMotif(gold, paleGold, 0.5F, Easing.SINE_IN_OUT),
                () -> (SpiritShardItem) CurrencyItems.item(CurrencyIdentity.TEMPO).get())
                .setItemColor(gold).build()));
        for (var entry : TYPES.entrySet()) {
            if (entry.getKey() == CurrencyIdentity.TEMPO) continue;
            CurrencyIdentity identity = entry.getKey();
            ((MalumSpiritTypeAccessor) entry.getValue()).betterContentEconomy$setSpiritShard(
                    () -> (SpiritShardItem) CurrencyItems.item(identity).get());
        }
    }

    public static MalumSpiritType type(CurrencyIdentity identity) {
        initialize();
        return TYPES.get(identity);
    }
}
