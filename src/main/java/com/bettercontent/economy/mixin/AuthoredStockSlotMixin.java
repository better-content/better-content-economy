package com.bettercontent.economy.mixin;

import com.bettercontent.economy.trader.AuthoredMerchantStock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rechecks shared stock on the server at the last point before a merchant result is taken. */
@Mixin(Slot.class)
abstract class AuthoredStockSlotMixin {
    @Shadow @Final public Container container;
    @Shadow @Final private int slot;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$checkAuthoredStock(final Player player,
            final CallbackInfoReturnable<Boolean> callback) {
        if (!(player.level() instanceof ServerLevel level)
                || !(container instanceof MerchantContainer merchantContainer)
                || slot != 2
                || !((Object) this instanceof MerchantResultSlot)) return;

        Merchant merchant = ((MerchantContainerAccessor) merchantContainer).betterContentEconomy$getMerchant();
        MerchantOffer offer = merchantContainer.getActiveOffer();
        if (!AuthoredMerchantStock.canPickup(merchant, offer, level)) callback.setReturnValue(false);
    }
}
