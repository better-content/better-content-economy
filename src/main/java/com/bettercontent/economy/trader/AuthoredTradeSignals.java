package com.bettercontent.economy.trader;

import com.bettercontent.economy.compat.ThreadSignalsBridge;
import com.bettercontent.economy.curios.CoinPurseCurio;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Emits learning evidence after a successful coin payment against an economy-authored trader. */
public final class AuthoredTradeSignals {
    private AuthoredTradeSignals() {}

    @SubscribeEvent
    public static void traded(final TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isAuthoredCoinTrade(event.getAbstractVillager(), event.getMerchantOffer())) return;
        ThreadSignalsBridge.authoredCoinSpent(player);
    }

    static boolean isAuthoredCoinTrade(final AbstractVillager trader, final MerchantOffer offer) {
        if (!(trader instanceof Villager) && !(trader instanceof WanderingTrader)) return false;
        return offer != null && (CoinPurseCurio.isCoin(offer.getCostA()) || CoinPurseCurio.isCoin(offer.getCostB()));
    }
}
