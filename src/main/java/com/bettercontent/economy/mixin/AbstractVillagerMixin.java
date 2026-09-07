package com.bettercontent.economy.mixin;

import com.bettercontent.economy.trader.MerchantCurrencyPolicy;
import com.bettercontent.economy.trader.VillageStarterOffer;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
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
        if (MerchantCurrencyPolicy.isExternalMerchantType(merchant)) {
            MerchantCurrencyPolicy.normalize(offers);
        }
        if (merchant instanceof WanderingTrader trader
                && trader.getPersistentData().contains(WanderingTraderVisits.THEME_TAG)) {
            VillageStarterOffer.ensurePresent(offers);
        }
    }
}
