package com.bettercontent.economy.mixin;

import com.bettercontent.economy.trader.AuthoredMerchantStock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Debits custom Merchant sellers at the vanilla successful-trade call site. */
@Mixin(MerchantResultSlot.class)
abstract class AuthoredMerchantResultSlotMixin {
    @Shadow @Final private Merchant merchant;
    @Shadow @Final private Player player;

    @Redirect(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void betterContentEconomy$recordCustomMerchantTrade(final Merchant merchant,
            final MerchantOffer offer) {
        merchant.notifyTrade(offer);
        if (merchant instanceof AbstractVillager || !(player.level() instanceof ServerLevel level)) return;
        // The redirect is reached only inside the successful MerchantOffer.take branch.
        AuthoredMerchantStock.recordCustomMerchantTrade(merchant, offer, level);
    }
}
