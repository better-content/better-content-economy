package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class WanderingTraderCatalogueTest {
    @Test void catalogueOwnsEightThemesAndThirteenRowsEach() {
        assertEquals(8, WanderingTraderCatalogue.themeCount());
        assertEquals(104, WanderingTraderCatalogue.authoredRowCount());
    }
}
