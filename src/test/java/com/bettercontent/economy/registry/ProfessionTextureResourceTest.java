package com.bettercontent.economy.registry;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionTextureResourceTest {
    private static final List<String> PROFESSION_NAMES = List.of(
            "sacred_caretaker",
            "wicked_hexbinder",
            "arcane_runescribe",
            "aerial_courier",
            "aqueous_tidekeeper",
            "earthen_stonewarden",
            "infernal_stoker",
            "tempo_clocksmith"
    );

    @Test
    void everySpiritProfessionHasVillagerAndZombieVillagerTextures() {
        for (String professionName : PROFESSION_NAMES) {
            assertTextureExists("villager", professionName);
            assertTextureExists("zombie_villager", professionName);
        }
    }

    private static void assertTextureExists(final String entityType, final String professionName) {
        final String path = "/assets/better_content_economy/textures/entity/"
                + entityType + "/profession/" + professionName + ".png";
        assertNotNull(ProfessionTextureResourceTest.class.getResource(path), "Missing texture " + path);
    }
}
