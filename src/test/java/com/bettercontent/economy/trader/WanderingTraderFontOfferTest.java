package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class WanderingTraderFontOfferTest {
    @BeforeAll static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            boolean expectedForgeHarnessFailure = java.util.stream.Stream.iterate((Throwable) error,
                            java.util.Objects::nonNull, Throwable::getCause)
                    .anyMatch(cause -> cause instanceof NoSuchMethodException
                            && cause.getMessage() != null
                            && cause.getMessage().startsWith("net.minecraftforge.network.NetworkEvent"));
            if (!expectedForgeHarnessFailure) throw error;
        }
    }

    @Test void absentProviderOfferLeavesExistingCatalogueUntouched() {
        MerchantOffers offers = new MerchantOffers();
        MerchantOffer existing = new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD), 3, 1, 0);
        offers.add(existing);

        assertFalse(WanderingTraderCatalogue.appendFontOffer(offers, null));
        assertEquals(1, offers.size());
        assertSame(existing, offers.get(0));
    }

    @Test void surveyedMapMarkerIsAddedOnlyOnce() {
        MerchantOffers offers = new MerchantOffers();
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.getOrCreateTag().putString("dimension_drink:font_definition_id", "nether");
        MerchantOffer mapOffer = new MerchantOffer(new ItemStack(Items.EMERALD, 8), map, 8, 6, 0);

        assertTrue(WanderingTraderCatalogue.appendFontOffer(offers, mapOffer));
        assertFalse(WanderingTraderCatalogue.appendFontOffer(offers, mapOffer));
        assertEquals(1, offers.size());
    }
}
