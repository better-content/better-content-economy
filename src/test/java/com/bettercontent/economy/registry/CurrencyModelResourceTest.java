package com.bettercontent.economy.registry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CurrencyModelResourceTest {
    @Test
    void everyEconomyCurrencyHasARegisteredItemModelAndTempoHasABlockstate() throws Exception {
        Map<String, String> parents = Map.of(
                "impact_spirit", "malum:item/infernal_spirit",
                "tempo_spirit", "malum:item/aerial_spirit",
                "work_spirit", "malum:item/arcane_spirit",
                "mobility_spirit", "malum:item/aerial_spirit",
                "endurance_spirit", "malum:item/aqueous_spirit",
                "robustness_spirit", "malum:item/earthen_spirit",
                "renewal_spirit", "malum:item/sacred_spirit",
                "control_spirit", "malum:item/wicked_spirit");
        Path models = Path.of("src/main/resources/assets/better_content_economy/models/item");
        parents.forEach((item, parent) -> {
            try {
                var model = JsonParser.parseString(Files.readString(models.resolve(item + ".json"))).getAsJsonObject();
                assertEquals(parent, model.get("parent").getAsString(), item);
            } catch (Exception error) {
                throw new AssertionError("Missing or unreadable model for " + item, error);
            }
        });

        Path blockstate = Path.of("src/main/resources/assets/better_content_economy/blockstates/tempo_metronome.json");
        Path blockModel = Path.of("src/main/resources/assets/better_content_economy/models/block/tempo_metronome.json");
        assertTrue(Files.isRegularFile(blockstate));
        assertTrue(Files.isRegularFile(blockModel));
        assertEquals(
                "better_content_economy:block/tempo_metronome",
                JsonParser.parseString(Files.readString(blockstate)).getAsJsonObject()
                        .getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
    }
}
