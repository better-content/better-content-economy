package com.bettercontent.spiritcommerce.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bettercontent.spiritcommerce.spirit.SpiritKind;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class FontTradeAffinityTest {
    @Test void eachSpiritMarketSellsOnlyDestinationsWithMatchingSalience() {
        Map<SpiritKind, Set<String>> expected = Map.of(
                SpiritKind.SACRED, Set.of("aether", "bumblezone"),
                SpiritKind.WICKED, Set.of("nether"),
                SpiritKind.ARCANE, Set.of("ratlantis"),
                SpiritKind.AERIAL, Set.of("aether"),
                SpiritKind.AQUEOUS, Set.of("ratlantis"),
                SpiritKind.EARTHEN, Set.of("bumblezone", "ratlantis"),
                SpiritKind.INFERNAL, Set.of("nether"),
                SpiritKind.TEMPO, Set.of("bumblezone"));
        for (SpiritKind kind : SpiritKind.values()) assertEquals(expected.get(kind), FontTradeAffinity.destinations(kind));
    }
}
