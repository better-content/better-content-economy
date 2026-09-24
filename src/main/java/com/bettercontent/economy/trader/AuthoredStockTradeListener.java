package com.bettercontent.economy.trader;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Debits once from the authoritative successful-trade event after the pickup guard passed. */
@Mod.EventBusSubscriber(modid = "better_content_economy")
public final class AuthoredStockTradeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthoredStockTradeListener.class);

    private AuthoredStockTradeListener() {}

    @SubscribeEvent
    public static void onTrade(final TradeWithVillagerEvent event) {
        AbstractVillager merchant = event.getAbstractVillager();
        if (!AuthoredMerchantStock.manages(merchant, event.getMerchantOffer())) return;
        if (!AuthoredMerchantStock.recordTrade(merchant, event.getMerchantOffer())) {
            LOGGER.error("Completed authored merchant trade without available finite stock: merchant={}, offer={}",
                    merchant.getType(), AuthoredOfferStock.key(event.getMerchantOffer()));
        }
    }
}
