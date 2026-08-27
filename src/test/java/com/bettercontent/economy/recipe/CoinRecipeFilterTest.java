package com.bettercontent.economy.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class CoinRecipeFilterTest {
    @Test void identifiesPrimitiveAndStructuredCoinOutputsOnly() {
        assertTrue(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString("{\"result\":\"createdeco:copper_coin\"}")));
        assertTrue(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString("{\"results\":[{\"item\":\"createdeco:gold_coinstack\"}]}")));
        assertFalse(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString(
                "{\"ingredient\":{\"item\":\"createdeco:copper_coin\"},\"result\":{\"item\":\"minecraft:stick\"}}")));
    }
}
