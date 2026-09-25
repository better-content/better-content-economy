package com.bettercontent.economy.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CoinRecipeFilterTest {
    @Test void identifiesPrimitiveAndStructuredCoinOutputsOnly() {
        assertTrue(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString("{\"result\":\"createdeco:copper_coin\"}")));
        assertTrue(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString("{\"results\":[{\"item\":\"createdeco:gold_coinstack\"}]}")));
        assertFalse(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString("{\"output\":{\"item\":\"malum:arcane_spirit\"}}")));
        assertFalse(CoinRecipeFilter.hasCoinOutput(JsonParser.parseString(
                "{\"ingredient\":{\"item\":\"createdeco:copper_coin\"},\"result\":{\"item\":\"minecraft:stick\"}}")));
    }

    @Test void rewritesMalumRecipeInputsAndOutputsButKeepsExoticSpirits() {
        var recipe = JsonParser.parseString("{\"secondaryInput\":{\"item\":\"malum:arcane_spirit\"},"
                + "\"output\":{\"item\":\"malum:infernal_spirit\"},"
                + "\"spirits\":[\"malum:eldritch_spirit\"]}");
        var result = CoinRecipeFilter.filter(Map.of(new ResourceLocation("malum", "runeworking/test"), recipe))
                .get(new ResourceLocation("malum", "runeworking/test")).getAsJsonObject();
        assertTrue(result.toString().contains("better_content_economy:work_spirit"));
        assertTrue(result.toString().contains("better_content_economy:impact_spirit"));
        assertTrue(result.toString().contains("malum:eldritch_spirit"));
    }
}
