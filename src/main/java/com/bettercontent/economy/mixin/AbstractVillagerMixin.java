package com.bettercontent.economy.mixin;

import com.bettercontent.economy.trader.MerchantCurrencyPolicy;
import com.bettercontent.economy.trader.PlagueDoctorCatalogue;
import com.bettercontent.economy.trader.VillageStarterOffer;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import com.bettercontent.economy.trader.WanderingTraderCatalogue;
import com.bettercontent.economy.trader.AuthoredOfferStock;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
    @Inject(method = "restock", at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$doNotAmbientlyRestockAuthoredOffers(final CallbackInfo callback) {
        AbstractVillager merchant = (AbstractVillager) (Object) this;
        if (merchant.getOffers().stream().anyMatch(offer -> AuthoredOfferStock.isFiniteAuthoredResult(offer.getResult()))) {
            callback.cancel();
        }
    }
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void betterContentEconomy$applyTradePolicy(final CallbackInfoReturnable<MerchantOffers> callback) {
        AbstractVillager merchant = (AbstractVillager) (Object) this;
        MerchantOffers offers = callback.getReturnValue();
        MerchantCurrencyPolicy.normalize(offers);
        if (PlagueDoctorCatalogue.isPlagueDoctor(merchant)) {
            PlagueDoctorCatalogue.ensureOffers(merchant, offers);
        }
        if (merchant instanceof WanderingTrader trader
                && trader.getPersistentData().contains(WanderingTraderVisits.THEME_TAG)) {
            WanderingTraderCatalogue.ensureThemedOffers(trader, offers);
            VillageStarterOffer.ensurePresent(trader, offers);
        }
    }
}
