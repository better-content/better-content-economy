package com.bettercontent.economy.mixin;

import com.bettercontent.economy.trader.MerchantCurrencyPolicy;
import com.bettercontent.economy.trader.PlagueDoctorCatalogue;
import com.bettercontent.economy.trader.VillageStarterOffer;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import com.bettercontent.economy.trader.WanderingTraderCatalogue;
import com.bettercontent.economy.trader.VillagerCatalogue;
import com.bettercontent.economy.resident.ResidentRules;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void betterContentEconomy$applyTradePolicy(final CallbackInfoReturnable<MerchantOffers> callback) {
        AbstractVillager merchant = (AbstractVillager) (Object) this;
        MerchantOffers offers = callback.getReturnValue();
        if (merchant instanceof Villager && ResidentRules.isResident(merchant)) {
            offers.clear();
            return;
        }
        MerchantCurrencyPolicy.normalize(offers);
        if (PlagueDoctorCatalogue.isPlagueDoctor(merchant)) {
            PlagueDoctorCatalogue.ensureOffers(merchant, offers);
        }
        if (merchant instanceof WanderingTrader trader
                && trader.getPersistentData().contains(WanderingTraderVisits.THEME_TAG)) {
            WanderingTraderCatalogue.ensureThemedOffers(trader, offers);
            VillageStarterOffer.ensurePresent(trader, offers);
        }
        if (merchant instanceof Villager villager) {
            VillagerCatalogue.ensureFontOfferIfAvailable(villager, offers);
        }
    }
}
