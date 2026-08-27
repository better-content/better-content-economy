package com.bettercontent.economy.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Removes every recipe whose output is a coin or coin stack; coin tiers never convert or craft. */
public final class CoinRecipeFilter {
    private static final Set<String> OUTPUTS = Set.of(
            "createdeco:copper_coin", "createdeco:iron_coin", "createdeco:industrial_iron_coin",
            "createdeco:brass_coin", "createdeco:gold_coin", "createdeco:zinc_coin",
            "createdeco:netherite_coin", "createdeco:copper_coinstack", "createdeco:iron_coinstack",
            "createdeco:industrial_iron_coinstack", "createdeco:brass_coinstack",
            "createdeco:gold_coinstack", "createdeco:zinc_coinstack", "createdeco:netherite_coinstack");

    private CoinRecipeFilter() {}

    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> recipes) {
        Map<ResourceLocation, JsonElement> filtered = new LinkedHashMap<>();
        recipes.forEach((id, json) -> {
            if (!hasCoinOutput(json)) filtered.put(id, json);
        });
        return filtered;
    }

    static boolean hasCoinOutput(JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        JsonObject root = recipe.getAsJsonObject();
        return matchesOutput(root.get("result")) || matchesOutput(root.get("output")) || matchesOutput(root.get("results"));
    }

    private static boolean matchesOutput(JsonElement element) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) return OUTPUTS.contains(element.getAsString());
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (matchesOutput(child)) return true;
            return false;
        }
        JsonObject object = element.getAsJsonObject();
        for (String key : List.of("item", "id", "result", "output")) {
            if (matchesOutput(object.get(key))) return true;
        }
        return false;
    }
}
