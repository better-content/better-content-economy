package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class CurrencyIdentityTest {
    @Test
    void legacyNativeSpiritsMapOnceWhileTempoHasNoNativeAlias() {
        assertEquals(CurrencyIdentity.RENEWAL, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:sacred_spirit")));
        assertEquals(CurrencyIdentity.CONTROL, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:wicked_spirit")));
        assertEquals(CurrencyIdentity.WORK, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:arcane_spirit")));
        assertEquals(CurrencyIdentity.MOBILITY, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:aerial_spirit")));
        assertEquals(CurrencyIdentity.ENDURANCE, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:aqueous_spirit")));
        assertEquals(CurrencyIdentity.ROBUSTNESS, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:earthen_spirit")));
        assertEquals(CurrencyIdentity.IMPACT, CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:infernal_spirit")));
        assertNull(CurrencyIdentity.TEMPO.legacyNativeSpirit());
        assertNull(CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:eldritch_spirit")));
        assertNull(CurrencyIdentity.fromLegacyNativeSpirit(new ResourceLocation("malum:umbral_spirit")));
    }

    @Test
    void onlyTheEightEconomyItemIdsQualifyForPouchCompatibility() {
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            assertEquals(identity, CurrencyIdentity.fromItemId(identity.itemId()));
        }
        assertNull(CurrencyIdentity.fromItemId(new ResourceLocation("malum:sacred_spirit")));
        assertNull(CurrencyIdentity.fromItemId(new ResourceLocation("better_content_economy:not_currency")));
    }
}
