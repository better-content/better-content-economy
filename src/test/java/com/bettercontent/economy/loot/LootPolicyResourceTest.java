package com.bettercontent.economy.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class LootPolicyResourceTest {
    @Test void everyEmeraldReplacementIsPackagedAndIndexed() throws Exception {
        var stream = getClass().getResourceAsStream("/data/forge/loot_modifiers/global_loot_modifiers.json");
        assertNotNull(stream);
        var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        var entries = root.getAsJsonArray("entries");
        assertEquals(136, entries.size());
        for (var entry : entries) {
            String path = entry.getAsString().substring("better_content_economy:".length());
            assertNotNull(getClass().getResourceAsStream(
                    "/data/better_content_economy/loot_modifiers/" + path + ".json"));
        }
    }
}
