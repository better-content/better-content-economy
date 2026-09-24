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
import com.bettercontent.economy.spirit.CurrencyIdentity;

/** Emits learning evidence after a successful authored spirit payment. */
public final class AuthoredTradeSignals {
    private AuthoredTradeSignals() {}

    @SubscribeEvent
    public static void traded(final TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SpiritPayment payment = paidSpirit(event.getMerchantOffer());
        if (payment == null || !isAuthored(event.getAbstractVillager())) return;
        CurrencyIdentity identity = mappedCurrency(payment.spirit());
        ResourceLocation merchant = ForgeRegistries.ENTITY_TYPES.getKey(event.getAbstractVillager().getType());
        if (identity != null && merchant != null) {
            ObservationalEconomyData observations = ObservationalEconomyData.get(player.server.overworld());
            observations.recordPurchase(identity, payment.count());
            observations.recordExchange(identity, payment.count(), merchant.toString());
        }
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

    /** Maps only the eight authored currencies; the returned item id remains available to events. */
    static CurrencyIdentity mappedCurrency(final ResourceLocation id) {
        CurrencyIdentity current = CurrencyIdentity.fromItemId(id);
        return current != null ? current : CurrencyIdentity.fromLegacyNativeSpirit(id);
    }

    private static ResourceLocation spiritId(final ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().endsWith("_spirit") ? id : null;
    }

    record SpiritPayment(ResourceLocation spirit, int count) {}
}
