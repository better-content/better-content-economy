package com.bettercontent.economy.recipe;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Removes retired coin and superseded Malum equipment recipes at the reload boundary. */
public final class CoinRecipeFilter {
    private static final java.util.Set<ResourceLocation> RETIRED_RECIPES = java.util.Set.of(
            id("malum:crude_scythe"), id("malum:soul_stained_steel_axe"),
            id("malum:soul_stained_steel_hoe"), id("malum:soul_stained_steel_pickaxe"),
            id("malum:soul_stained_steel_shovel"), id("malum:soul_stained_steel_sword"),
            id("malum:malum/soul_stained_steel_knife"), id("malum:spirit_infusion/soul_stained_steel_scythe"),
            id("malum:spirit_infusion/mnemonic_hex_staff"), id("malum:spirit_infusion/staff_of_the_auric_flame"));

    private CoinRecipeFilter() {}

    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> recipes) {
        Map<ResourceLocation, JsonElement> filtered = new LinkedHashMap<>();
        recipes.forEach((id, json) -> {
            if (RETIRED_RECIPES.contains(id) || ("better_content_economy".equals(id.getNamespace())
                    && id.getPath().startsWith("spirit_refinement/"))) return;
            JsonElement replacement = replaceOrdinarySpirits(json);
            if (!hasCoinOutput(replacement)) filtered.put(id, replacement);
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
        if (element.isJsonPrimitive()) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            return id != null && EconomyPolicy.isRetired(id);
        }
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

    private static ResourceLocation id(String value) { return new ResourceLocation(value); }

    /** Changes exact ordinary Malum item IDs in loaded recipes, including third-party recipes. */
    static JsonElement replaceOrdinarySpirits(JsonElement element) {
        if (element == null || element.isJsonNull()) return element;
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                CurrencyIdentity identity = CurrencyIdentity.fromLegacyNativeSpirit(id);
                if (identity != null) return new com.google.gson.JsonPrimitive(identity.itemId().toString());
            }
            return element.deepCopy();
        }
        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(replaceOrdinarySpirits(child)));
            return result;
        }
        JsonObject result = new JsonObject();
        element.getAsJsonObject().entrySet().forEach(entry ->
                result.add(entry.getKey(), replaceOrdinarySpirits(entry.getValue())));
        return result;
    }
}
