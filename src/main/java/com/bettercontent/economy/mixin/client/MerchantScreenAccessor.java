package com.bettercontent.economy.mixin.client;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("shopItem")
    void bettercontent$setShopItem(int index);

    @Accessor("scrollOff")
    void bettercontent$setScrollOff(int offset);
}
