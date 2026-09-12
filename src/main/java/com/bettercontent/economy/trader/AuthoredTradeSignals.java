package com.bettercontent.economy.trader;

import com.bettercontent.economy.api.event.AuthoredSpiritTradeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Emits learning evidence after a successful authored spirit payment. */
public final class AuthoredTradeSignals {
    private AuthoredTradeSignals() {}

    @SubscribeEvent
    public static void traded(final TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation spirit = paidSpirit(event.getMerchantOffer());
        if (spirit == null || !isAuthored(event.getAbstractVillager())) return;
        ResourceLocation merchant = ForgeRegistries.ENTITY_TYPES.getKey(event.getAbstractVillager().getType());
        MinecraftForge.EVENT_BUS.post(new AuthoredSpiritTradeEvent(
                player, spirit, event.getMerchantOffer().getCostA().getCount(), merchant));
    }

    static boolean isAuthored(final AbstractVillager trader) {
        return trader instanceof Villager || trader instanceof WanderingTrader
                || PlagueDoctorCatalogue.isPlagueDoctor(trader);
    }

    private static ResourceLocation paidSpirit(final MerchantOffer offer) {
        ResourceLocation first = spiritId(offer.getCostA());
        return first != null ? first : spiritId(offer.getCostB());
    }

    private static ResourceLocation spiritId(final ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "malum".equals(id.getNamespace()) && id.getPath().endsWith("_spirit") ? id : null;
    }
}
