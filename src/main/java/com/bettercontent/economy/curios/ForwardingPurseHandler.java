package com.bettercontent.economy.curios;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Keeps the inventory-menu slot layout stable while the client is still waiting for Curios
 * capability synchronization.
 */
public final class ForwardingPurseHandler implements IItemHandlerModifiable {
    private final Supplier<Optional<? extends IItemHandler>> delegate;
    private IItemHandler resolved;

    public ForwardingPurseHandler(final Player player) {
        this(() -> CoinPurseCurio.stacks(player));
    }

    ForwardingPurseHandler(final Supplier<Optional<? extends IItemHandler>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return CoinPurseCurio.SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return current().map(handler -> handler.getStackInSlot(slot)).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return current().map(handler -> handler.insertItem(slot, stack, simulate)).orElse(stack);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return current().map(handler -> handler.extractItem(slot, amount, simulate)).orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return current().map(handler -> handler.getSlotLimit(slot)).orElse(64);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return CoinPurseCurio.isCoin(stack)
                && current().map(handler -> handler.isItemValid(slot, stack)).orElse(false);
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        current().filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast)
                .ifPresent(handler -> handler.setStackInSlot(slot, stack));
    }

    private Optional<? extends IItemHandler> current() {
        if (resolved != null) return Optional.of(resolved);
        Optional<? extends IItemHandler> current = delegate.get()
                .filter(handler -> handler.getSlots() >= CoinPurseCurio.SLOT_COUNT);
        current.ifPresent(handler -> resolved = handler);
        return current;
    }
}
