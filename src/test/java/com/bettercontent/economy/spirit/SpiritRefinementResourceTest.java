package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class SpiritRefinementResourceTest {
    private static final Set<String> CURRENCY_IDS = Set.of(
            "better_content_economy:impact_spirit",
            "better_content_economy:tempo_spirit",
            "better_content_economy:work_spirit",
            "better_content_economy:mobility_spirit",
            "better_content_economy:endurance_spirit",
            "better_content_economy:robustness_spirit",
            "better_content_economy:renewal_spirit",
            "better_content_economy:control_spirit");

    private static final Set<String> NATIVE_REAGENT_IDS = Set.of(
            "malum:infernal_spirit",
            "malum:aerial_spirit",
            "malum:arcane_spirit",
            "malum:aqueous_spirit",
            "malum:earthen_spirit",
            "malum:sacred_spirit",
            "malum:wicked_spirit");

    @Test
    void everyCurrencyHasOneWayNativeSpiritRefinement() throws Exception {
        final Path directory = Path.of("src/main/resources/data/better_content_economy/recipes/spirit_refinement");
        final Map<String, Integer> inputCounts = new HashMap<>();
        final Map<String, Integer> outputCounts = new HashMap<>();
        int recipeCount = 0;

        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                final JsonObject recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertEquals("malum:spirit_focusing", recipe.get("type").getAsString(), path.toString());
                final String input = recipe.getAsJsonObject("input").get("item").getAsString();
                final String output = recipe.getAsJsonObject("output").get("item").getAsString();

                inputCounts.merge(input, 1, Integer::sum);
                outputCounts.merge(output, 1, Integer::sum);
                recipeCount++;

                assertTrue(CURRENCY_IDS.contains(input), "unexpected refinement input: " + input);
                assertTrue(NATIVE_REAGENT_IDS.contains(output), "unexpected refinement output: " + output);
                assertEquals(1, recipe.get("durabilityCost").getAsInt(), path.toString());
                assertEquals(300, recipe.get("time").getAsInt(), path.toString());
                assertTrue(recipe.getAsJsonArray("spirits").isEmpty(), path.toString());
            }
        }

        assertEquals(CURRENCY_IDS.size(), recipeCount);
        assertEquals(CURRENCY_IDS, inputCounts.keySet());
        assertTrue(inputCounts.values().stream().allMatch(count -> count == 1), "each currency must have exactly one refinement");
        assertEquals(NATIVE_REAGENT_IDS, outputCounts.keySet());
        assertEquals(2, outputCounts.get("malum:aerial_spirit"), "aerial is the native mapping shared by mobility and tempo");
        assertTrue(outputCounts.values().stream().allMatch(count -> count == 1 || count == 2));
        assertTrue(inputCounts.keySet().stream().noneMatch(outputCounts::containsKey), "refinements must not form currency/reverse loops");
    }
}
