package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class AuthoredStockLedgerTest {
    @Test void purchaseIsFiniteAndPersistsAcrossReload() {
        AuthoredStockLedger ledger = new AuthoredStockLedger();
        ledger.seed("diamond", 2);
        assertTrue(ledger.purchase("diamond", 1));
        assertFalse(ledger.purchase("diamond", 2));
        AuthoredStockLedger reloaded = AuthoredStockLedger.load(ledger.save());
        assertEquals(1, reloaded.remaining("diamond"));
    }

    @Test void rejectedPurchaseDoesNotMutateAndExplicitReplenishmentRestoresStock() {
        AuthoredStockLedger ledger = new AuthoredStockLedger();
        ledger.seed("netherite", 1);
        assertFalse(ledger.purchase("netherite", 2));
        assertEquals(1, ledger.remaining("netherite"));
        ledger.replenish("netherite", 2);
        assertEquals(3, ledger.remaining("netherite"));
    }
}
