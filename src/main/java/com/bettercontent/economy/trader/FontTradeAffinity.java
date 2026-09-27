package com.bettercontent.economy.trader;

import com.bettercontent.economy.spirit.SpiritKind;
import java.util.Set;

/** Surveyed Font destinations whose salience aspects fit each spirit market. */
final class FontTradeAffinity {
    private FontTradeAffinity() {}

    static Set<String> destinations(final SpiritKind kind) {
        return switch (kind) {
            case SACRED -> Set.of("aether", "bumblezone");
            case WICKED, INFERNAL -> Set.of("nether");
            case ARCANE, AQUEOUS -> Set.of("ratlantis");
            case AERIAL -> Set.of("aether");
            case EARTHEN -> Set.of("bumblezone", "ratlantis");
            case TEMPO -> Set.of("bumblezone");
        };
    }
}
