package com.bettercontent.economy.trader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMarketValidationTest {
    private static final String IDENTITY = "real-offer-and-price";

    private boolean accepts(boolean merchant, boolean alive, boolean sameDimension, double distanceSquared,
                            int index, boolean offerExists, boolean inStock, int uses, int maxUses, String identity) {
        return LocalMarket.accepts(merchant, alive, sameDimension, distanceSquared, index,
                offerExists, inStock, true, uses, maxUses, identity, IDENTITY);
    }

    @Test void acceptsOnlyTheSameLiveNearbyInStockOffer() {
        assertTrue(accepts(true, true, true, 64, 0, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, true, false, 1, 0, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 64.01, 0, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(false, true, true, 1, 0, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, false, true, 1, 0, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 1, -1, true, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 1, 4, false, true, 1, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 1, 0, true, false, 2, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 1, 0, true, true, 2, 2, IDENTITY));
        assertFalse(accepts(true, true, true, 1, 0, true, true, 0, 0, IDENTITY));
        assertFalse(accepts(true, true, true, 1, 0, true, true, 1, 2, "changed-price-or-offer"));
    }

    @Test void rejectsOfferWhenSharedFiniteStockWasExhaustedAfterSnapshot() {
        assertFalse(LocalMarket.accepts(true, true, true, 1, 0, true, true, false,
                0, 2, IDENTITY, IDENTITY));
        assertTrue(LocalMarket.accepts(true, true, true, 1, 0, true, true, true,
                0, 2, IDENTITY, IDENTITY));
    }
}
