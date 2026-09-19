package com.bettercontent.economy.trader;

import com.bettercontent.economy.api.event.AuthoredSpiritTradeEvent;
import com.bettercontent.economy.ops.ObservationalEconomyData;
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
        SpiritPayment payment = paidSpirit(event.getMerchantOffer());
        if (payment == null || !isAuthored(event.getAbstractVillager())) return;
        ObservationalEconomyData.get(player.server.overworld()).recordPurchase(
                com.bettercontent.economy.spirit.CurrencyIdentity.fromItemId(payment.spirit()), payment.count());
        ResourceLocation merchant = ForgeRegistries.ENTITY_TYPES.getKey(event.getAbstractVillager().getType());
        MinecraftForge.EVENT_BUS.post(new AuthoredSpiritTradeEvent(
                player, payment.spirit(), payment.count(), merchant));
    }

    static boolean isAuthored(final AbstractVillager trader) {
        return trader instanceof Villager || trader instanceof WanderingTrader
                || PlagueDoctorCatalogue.isPlagueDoctor(trader);
    }

    static SpiritPayment paidSpirit(final MerchantOffer offer) {
        ResourceLocation first = spiritId(offer.getCostA());
        if (first != null) return new SpiritPayment(first, offer.getCostA().getCount());
        ResourceLocation second = spiritId(offer.getCostB());
        return second == null ? null : new SpiritPayment(second, offer.getCostB().getCount());
    }

    private static ResourceLocation spiritId(final ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "malum".equals(id.getNamespace()) && id.getPath().endsWith("_spirit") ? id : null;
    }

    record SpiritPayment(ResourceLocation spirit, int count) {}
}
