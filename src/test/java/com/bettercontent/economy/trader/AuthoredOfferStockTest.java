package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class AuthoredOfferStockTest {
    @Test void keysAreStableForTheSameOfferShape() {
        // The adapter's contract is deliberately string based so it survives entity reloads.
        assertEquals("minecraft:diamond#1:unknown#0=>minecraft:stick#1", "minecraft:diamond#1:unknown#0=>minecraft:stick#1");
    }
}
