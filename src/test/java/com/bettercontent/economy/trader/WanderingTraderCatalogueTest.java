package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class WanderingTraderCatalogueTest {
    @Test void catalogueOwnsSevenThemesAndThirteenRowsEach() {
        assertEquals(7, WanderingTraderCatalogue.themeCount());
        assertEquals(91, WanderingTraderCatalogue.authoredRowCount());
    }
}
