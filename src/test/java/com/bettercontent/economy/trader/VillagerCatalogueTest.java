package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.economy.spirit.SpiritKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.npc.VillagerTrades;
import org.junit.jupiter.api.Test;

final class VillagerCatalogueTest {
    @Test void catalogueHasThirtyFiveRowsForEverySpiritProfession() {
        assertEquals(280, VillagerCatalogue.rowCount());
        assertTrue(VillagerCatalogue.allRowsUseExactlyOneSpiritSide());
    }

    @Test void nonEconomyProfessionPreservesExistingListings() {
        Map<Integer, List<VillagerTrades.ItemListing>> trades = new HashMap<>();
        VillagerTrades.ItemListing sentinel = (entity, random) -> null;
        trades.put(1, new ArrayList<>(List.of(sentinel)));

        assertFalse(VillagerCatalogue.replaceListings(null, trades));

        assertEquals(1, trades.size());
        assertEquals(List.of(sentinel), trades.get(1));
    }

    @Test void authoredReplacementReplacesListingsForAllEightEconomyProfessions() {
        assertEquals(8, SpiritKind.values().length);
        for (SpiritKind kind : SpiritKind.values()) {
            Map<Integer, List<VillagerTrades.ItemListing>> trades = new HashMap<>();
            VillagerTrades.ItemListing sentinel = (entity, random) -> null;
            trades.put(1, new ArrayList<>(List.of(sentinel)));

            assertTrue(VillagerCatalogue.replaceListings(kind, trades), kind.id());

            assertFalse(trades.values().stream().anyMatch(listings -> listings.contains(sentinel)), kind.id());
            assertEquals(35, trades.values().stream().mapToInt(List::size).sum(), kind.id());
        }
    }
}
