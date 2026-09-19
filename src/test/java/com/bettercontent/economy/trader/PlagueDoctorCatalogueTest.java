package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bettercontent.economy.config.EconomyPolicy;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class PlagueDoctorCatalogueTest {
    private static final UUID DOCTOR = UUID.fromString("f390b5bd-c3ab-4d1e-9be0-f1f1eb11f69e");

    @Test void dailySelectionIsStableUniqueAndRotates() {
        var first = PlagueDoctorCatalogue.selectRows(DOCTOR, 42L, ignored -> true);
        var repeated = PlagueDoctorCatalogue.selectRows(DOCTOR, 42L, ignored -> true);
        var nextDay = PlagueDoctorCatalogue.selectRows(DOCTOR, 43L, ignored -> true);
        assertEquals(first, repeated);
        assertEquals(8, first.size());
        assertEquals(8, first.stream().map(row -> row.result().id()).distinct().count());
        assertNotEquals(first, nextDay);
    }

    @Test void unavailableOptionalItemsAreSkippedWithoutDuplicates() {
        Set<ResourceLocation> availableResults = EconomyPolicy.plagueDoctorRows().stream().limit(3)
                .map(row -> new ResourceLocation(row.result().id())).collect(Collectors.toSet());
        var selected = PlagueDoctorCatalogue.selectRows(DOCTOR, 7L,
                id -> "better_content_economy".equals(id.getNamespace()) || availableResults.contains(id));
        assertEquals(3, selected.size());
        assertTrue(selected.stream().allMatch(row -> availableResults.contains(new ResourceLocation(row.result().id()))));
    }
}
