package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

final class CurrencyTooltipContractTest {
    @Test
    void everyCurrencyTooltipTeachesItsMerchantAndNativeRefinementUses() throws Exception {
        JsonObject language = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/better_content_economy/lang/en_us.json"))).getAsJsonObject();
        Path recipes = Path.of("src/main/resources/data/better_content_economy/recipes/spirit_refinement");

        for (CurrencyIdentity identity : CurrencyIdentity.values()) {
            Path recipePath = recipes.resolve(identity.id() + ".json");
            JsonObject recipe = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
            assertEquals(identity.itemId().toString(), recipe.getAsJsonObject("input").get("item").getAsString());

            String nativeSpirit = recipe.getAsJsonObject("output").get("item").getAsString();
            assertTrue(nativeSpirit.startsWith("malum:"), recipePath.toString());
            String instruction = language.get("tooltip.better_content_economy." + identity.id()).getAsString();
            String nativeName = displayName(nativeSpirit);
            String currencyName = identity.id().substring(0, 1).toUpperCase(Locale.ROOT)
                    + identity.id().substring(1);
            assertEquals(currencyName + " currency: trade with themed merchants, or focus into "
                    + article(nativeName) + " " + nativeName + ".", instruction, identity.id());
            assertTrue(instruction.split("\\s+").length <= 24, identity.id());
        }
    }

    private static String displayName(String itemId) {
        String path = itemId.substring(itemId.indexOf(':') + 1);
        StringBuilder name = new StringBuilder();
        for (String word : path.split("_")) {
            if (name.length() > 0) name.append(' ');
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.toString();
    }

    private static String article(String name) {
        return "AEIOUaeiou".indexOf(name.charAt(0)) >= 0 ? "an" : "a";
    }
}
