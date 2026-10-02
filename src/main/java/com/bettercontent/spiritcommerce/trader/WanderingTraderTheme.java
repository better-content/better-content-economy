package com.bettercontent.spiritcommerce.trader;

import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import com.bettercontent.spiritcommerce.spirit.SpiritKind;
import com.bettercontent.spiritcommerce.spirit.CurrencyIdentity;

public enum WanderingTraderTheme {
    SACRED,
    WICKED,
    ARCANE,
    AERIAL,
    AQUEOUS,
    EARTHEN,
    INFERNAL,
    TEMPO;

    private static final WanderingTraderTheme[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("entity.better_spirit_commerce.wandering_trader." + id());
    }

    public static WanderingTraderTheme fromIndex(final int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public static WanderingTraderTheme fromId(final String id) {
        for (WanderingTraderTheme theme : VALUES) {
            if (theme.id().equals(id)) {
                return theme;
            }
        }
        return null;
    }

    public static WanderingTraderTheme forUuid(final UUID uuid) {
        return fromIndex(uuid.hashCode());
    }

    public WanderingTraderTheme next() {
        return fromIndex(ordinal() + 1);
    }

    public SpiritKind spirit() {
        return SpiritKind.fromId(id());
    }

    /** The durable currency identity for this legacy themed trader. */
    public CurrencyIdentity currencyIdentity() {
        return spirit().currencyIdentity();
    }
}
