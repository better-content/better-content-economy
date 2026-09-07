package com.bettercontent.economy.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    protected InventoryScreenMixin() {
        super(null, null, Component.empty());
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterContentEconomy$extendForPurse(final Player player, final CallbackInfo callback) {
        imageWidth = 226;
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void betterContentEconomy$renderPurse(
            final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY,
            final CallbackInfo callback) {
        int left = leftPos + 176;
        int top = topPos + 17;
        graphics.fill(left, top, left + 50, top + 83, 0xFF8B8B8B);
        graphics.fill(left + 1, top + 1, left + 49, top + 82, 0xFFC6C6C6);
        for (int slot = 0; slot < 8; slot++) {
            int x = leftPos + 180 + (slot % 2) * 18;
            int y = topPos + 26 + (slot / 2) * 18;
            graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, slot == 7 ? 0xFF9E9E9E : 0xFF8B8B8B);
        }
    }

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void betterContentEconomy$renderPurseLabel(
            final GuiGraphics graphics, final int mouseX, final int mouseY, final CallbackInfo callback) {
        graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("gui.better_content_economy.coin_purse"),
                180,
                18,
                0x404040,
                false);
    }
}
