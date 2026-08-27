package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class WanderingTraderCatalogueTest {
    @Test void catalogueOwnsFourThemesAndNinetySixExactRows() {
        assertEquals(4, WanderingTraderCatalogue.themeCount());
        assertEquals(96, WanderingTraderCatalogue.authoredRowCount());
    }
}
