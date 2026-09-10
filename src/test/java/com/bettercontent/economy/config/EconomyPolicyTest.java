package com.bettercontent.economy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.economy.spirit.SpiritKind;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class EconomyPolicyTest {
    @Test void centralPolicyOwnsAllCommerceRowsAndRetiredItems() {
        assertEquals(245, EconomyPolicy.villagerRowCount());
        assertEquals(91, EconomyPolicy.wanderingRowCount());
        assertEquals(24, EconomyPolicy.retiredItems().size());
        assertTrue(EconomyPolicy.isRetired(new ResourceLocation("createdeco:copper_coin")));
        assertFalse(EconomyPolicy.isRetired(new ResourceLocation("malum:eldritch_spirit")));
        for (SpiritKind kind : SpiritKind.values()) {
            assertEquals(35, EconomyPolicy.villagerRows(kind).size());
            assertEquals(13, EconomyPolicy.wanderingRows(kind).size());
        }
    }
}
