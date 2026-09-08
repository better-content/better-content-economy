package com.bettercontent.economy.mixin;

import com.bettercontent.economy.curios.CoinPurseCurio;
import com.bettercontent.economy.curios.ForwardingPurseHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends RecipeBookMenu<CraftingContainer> {
    @Unique private int betterContentEconomy$purseStart;
    @Unique private int betterContentEconomy$purseEnd;

    protected InventoryMenuMixin() {
        super((MenuType<?>) null, 0);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterContentEconomy$addPurseSlots(
            final Inventory inventory, final boolean active, final Player owner, final CallbackInfo callback) {
        betterContentEconomy$purseStart = slots.size();
        betterContentEconomy$addPurseSlots(new ForwardingPurseHandler(owner));
        betterContentEconomy$purseEnd = slots.size();
    }

    @Unique
    private void betterContentEconomy$addPurseSlots(final ForwardingPurseHandler handler) {
        for (int slot = 0; slot < CoinPurseCurio.SLOT_COUNT; slot++) {
            int x = 181 + (slot % 2) * 18;
            int y = 27 + (slot / 2) * 18;
            addSlot(new PurseSlot(handler, slot, x, y));
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$quickMovePurse(
            final Player player, final int index, final CallbackInfoReturnable<ItemStack> callback) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) return;
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem()) return;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();

        boolean handled = slot instanceof PurseSlot
                ? moveItemStackTo(moving, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)
                : index >= InventoryMenu.INV_SLOT_START && index < InventoryMenu.USE_ROW_SLOT_END
                        && CoinPurseCurio.isCoin(moving)
                        && moveItemStackTo(
                                moving,
                                betterContentEconomy$purseStart,
                                betterContentEconomy$purseEnd,
                                false);
        if (!handled) return;

        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, moving);
        callback.setReturnValue(original);
    }

    private static final class PurseSlot extends SlotItemHandler {
        private PurseSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return CoinPurseCurio.isCoin(stack) && super.mayPlace(stack);
        }
    }
}
