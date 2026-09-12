package com.bettercontent.economy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the authored catalogue reset only when its calendar day changes. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.rats.server.entity.misc.PlagueDoctor", remap = false)
abstract class PlagueDoctorMixin {
    @Inject(method = "exhaustedAnyTrades", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void betterContentEconomy$disableNativeRestock(final CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
