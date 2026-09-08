package com.bettercontent.economy.curios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.junit.jupiter.api.Test;

final class ForwardingPurseHandlerTest {
    @Test
    void exposesStableSlotCountBeforeCapabilityArrivesAndForwardsAfterward() {
        AtomicReference<IItemHandler> delegate = new AtomicReference<>();
        AtomicInteger resolutions = new AtomicInteger();
        ForwardingPurseHandler handler = new ForwardingPurseHandler(() -> {
            resolutions.incrementAndGet();
            return Optional.ofNullable(delegate.get());
        });

        assertEquals(CoinPurseCurio.SLOT_COUNT, handler.getSlots());
        assertEquals(64, handler.getSlotLimit(0));

        AtomicInteger writes = new AtomicInteger();
        IItemHandler attached = new IItemHandlerModifiable() {
            @Override public int getSlots() { return CoinPurseCurio.SLOT_COUNT; }
            @Override public ItemStack getStackInSlot(int slot) { throw new UnsupportedOperationException(); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { throw new UnsupportedOperationException(); }
            @Override public int getSlotLimit(int slot) { return 17; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
            @Override public void setStackInSlot(int slot, ItemStack stack) { writes.incrementAndGet(); }
        };
        delegate.set(attached);
        assertEquals(17, handler.getSlotLimit(0));
        assertEquals(17, handler.getSlotLimit(1));
        handler.setStackInSlot(0, null);
        assertEquals(1, writes.get());
        assertEquals(2, resolutions.get());
    }
}
