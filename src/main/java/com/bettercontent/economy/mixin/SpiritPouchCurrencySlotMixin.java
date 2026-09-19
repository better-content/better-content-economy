package com.bettercontent.economy.mixin;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds only economy currencies to Malum's existing native-spirit Pouch slot predicate. */
@Mixin(targets = "com.sammy.malum.common.container.SpiritPouchContainer$1", remap = false)
abstract class SpiritPouchCurrencySlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void betterContentEconomy$acceptCurrency(final ItemStack stack,
                                                     final CallbackInfoReturnable<Boolean> callback) {
        if (CurrencyIdentity.fromItemId(ForgeRegistries.ITEMS.getKey(stack.getItem())) != null) {
            callback.setReturnValue(true);
        }
    }
}
