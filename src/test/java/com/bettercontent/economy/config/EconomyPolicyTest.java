package com.bettercontent.economy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.economy.spirit.SpiritKind;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class EconomyPolicyTest {
    @Test void centralPolicyOwnsAllCommerceRowsAndRetiredItems() {
        assertEquals(245, EconomyPolicy.villagerRowCount());
        assertEquals(91, EconomyPolicy.wanderingRowCount());
        assertEquals(42, EconomyPolicy.plagueDoctorRowCount());
        assertEquals(24, EconomyPolicy.retiredItems().size());
        assertTrue(EconomyPolicy.isRetired(new ResourceLocation("createdeco:copper_coin")));
        assertFalse(EconomyPolicy.isRetired(new ResourceLocation("malum:eldritch_spirit")));
        assertEquals("bc.economy_policy.v3", EconomyPolicy.SCHEMA);
        assertEquals(CurrencyIdentity.RENEWAL,
                CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:sacred_spirit")));
        for (SpiritKind kind : SpiritKind.values()) {
            assertEquals(35, EconomyPolicy.villagerRows(kind).size());
            assertEquals(13, EconomyPolicy.wanderingRows(kind).size());
            assertEquals(6, EconomyPolicy.plagueDoctorRows().stream().filter(row -> row.kind() == kind).count());
        }
        assertTrue(EconomyPolicy.plagueDoctorRows().stream()
                .anyMatch(row -> "rats:plague_tome".equals(row.result().id()) && row.cost() == 8));
        assertTrue(EconomyPolicy.plagueDoctorRows().stream().allMatch(row ->
                row.maxUses() == EconomyPolicy.plagueDoctorMaxUses(row.cost()) && row.xp() == row.cost() * 2));
    }
}
