package com.bettercontent.economy.mixin.client;

import com.bettercontent.economy.curios.CoinPurseLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/inventory.png");

    protected InventoryScreenMixin() {
        super(null, null, Component.empty());
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void betterContentEconomy$renderPurse(
            final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY,
            final CallbackInfo callback) {
        int top = topPos + CoinPurseLayout.INVENTORY_HEIGHT;
        graphics.fill(leftPos, top, leftPos + CoinPurseLayout.INVENTORY_WIDTH,
                top + CoinPurseLayout.STRIP_HEIGHT, 0xFF555555);
        graphics.fill(leftPos + 1, top, leftPos + CoinPurseLayout.INVENTORY_WIDTH - 1,
                top + 1, 0xFFFFFFFF);
        graphics.fill(leftPos + 1, top + 1, leftPos + CoinPurseLayout.INVENTORY_WIDTH - 1,
                top + CoinPurseLayout.STRIP_HEIGHT - 1, 0xFFC6C6C6);
        for (int slot = 0; slot < CoinPurseLayout.SLOT_COUNT; slot++) {
            int x = leftPos + CoinPurseLayout.slotX(slot) - 1;
            int y = topPos + CoinPurseLayout.SLOT_Y - 1;
            graphics.blit(INVENTORY_TEXTURE, x, y, 7, 83, 18, 18);
        }
    }

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void betterContentEconomy$renderPurseLabel(
            final GuiGraphics graphics, final int mouseX, final int mouseY, final CallbackInfo callback) {
        graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("gui.better_content_economy.coin_purse"),
                7,
                CoinPurseLayout.INVENTORY_HEIGHT + 7,
                0x404040,
                false);
    }
}
