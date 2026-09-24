package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.*;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class AuthoredTradeSignalsMappingTest {
    @Test void everyCurrentCurrencyMaps() {
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            assertSame(identity, AuthoredTradeSignals.mappedCurrency(identity.itemId()));
        }
    }

    @Test void everyLegacyNativeCurrencyMaps() {
        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            if (identity.legacyNativeSpirit() != null) assertSame(identity, AuthoredTradeSignals.mappedCurrency(identity.legacyNativeSpirit()));
        }
    }

    @Test void exoticAndUnknownSpiritsDoNotAggregateButRetainActualId() {
        ResourceLocation eldritch = new ResourceLocation("malum", "eldritch_spirit");
        ResourceLocation umbral = new ResourceLocation("malum", "umbral_spirit");
        ResourceLocation unknown = new ResourceLocation("other", "mystery_spirit");
        assertNull(AuthoredTradeSignals.mappedCurrency(eldritch));
        assertNull(AuthoredTradeSignals.mappedCurrency(umbral));
        assertNull(AuthoredTradeSignals.mappedCurrency(unknown));
        AuthoredTradeSignals.SpiritPayment eventPayment = new AuthoredTradeSignals.SpiritPayment(eldritch, 2);
        assertEquals(eldritch, eventPayment.spirit());
    }
}
