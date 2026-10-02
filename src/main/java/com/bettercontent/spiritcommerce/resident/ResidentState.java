package com.bettercontent.spiritcommerce.resident;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Small persistent inventory and needs shared by villagers and settlement rats. */
public final class ResidentState {
    private static final String KEY = "better_spirit_commerce:resident";
    private static final int SLOTS = 24;
    private final LivingEntity owner;
    private final List<ItemStack> items = new ArrayList<>(SLOTS);
    private int food;
    private int water;
    private int rest;
    private String doing;
    private final List<String> events = new ArrayList<>();

    private ResidentState(LivingEntity owner) {
        this.owner = owner;
        for (int i = 0; i < SLOTS; i++) items.add(ItemStack.EMPTY);
        CompoundTag tag = owner.getPersistentData().getCompound(KEY);
        food = tag.contains("Food") ? tag.getInt("Food") : 18;
        water = tag.contains("Water") ? tag.getInt("Water") : 18;
        rest = tag.contains("Rest") ? tag.getInt("Rest") : 18;
        doing = tag.getString("Doing");
        for (Tag raw : tag.getList("Events", Tag.TAG_STRING)) events.add(raw.getAsString());
        for (Tag raw : tag.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            int slot = row.getByte("Slot") & 255;
            if (slot < SLOTS) items.set(slot, ItemStack.of(row));
        }
    }

    public static ResidentState of(LivingEntity owner) { return new ResidentState(owner); }
    public int food() { return food; }
    public int water() { return water; }
    public int rest() { return rest; }
    public String doing() { return doing; }
    public List<String> events() { return List.copyOf(events); }
    public List<ItemStack> items() { return items.stream().map(ItemStack::copy).toList(); }
    public void setDoing(String value) { if (!doing.equals(value)) { doing = value; save(); } }
    public void event(String value) {
        events.add(owner.level().getGameTime() + " " + value);
        if (events.size() > 32) events.remove(0);
        save();
    }
    public void setNeeds(int food, int water, int rest) {
        this.food = Math.max(0, Math.min(20, food));
        this.water = Math.max(0, Math.min(20, water));
        this.rest = Math.max(0, Math.min(20, rest));
        save();
    }

    public int count(ItemStack wanted) {
        int count = 0;
        for (ItemStack stack : items) if (ItemStack.isSameItemSameTags(stack, wanted)) count += stack.getCount();
        return count;
    }

    public boolean remove(ItemStack wanted) {
        if (wanted.isEmpty() || count(wanted) < wanted.getCount()) return false;
        int remaining = wanted.getCount();
        for (int i = 0; i < SLOTS && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (!ItemStack.isSameItemSameTags(stack, wanted)) continue;
            int taken = Math.min(stack.getCount(), remaining);
            stack.shrink(taken);
            remaining -= taken;
            if (stack.isEmpty()) items.set(i, ItemStack.EMPTY);
        }
        save();
        return true;
    }

    public boolean canAdd(ItemStack incoming) {
        int remaining = incoming.getCount();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) remaining -= incoming.getMaxStackSize();
            if (ItemStack.isSameItemSameTags(stack, incoming)) remaining -= stack.getMaxStackSize() - stack.getCount();
            if (remaining <= 0) return true;
        }
        return false;
    }

    /** Commit a fully validated inventory snapshot in one save. */
    void replaceItems(List<ItemStack> snapshot) {
        if (snapshot.size() != SLOTS) throw new IllegalArgumentException("Invalid resident inventory");
        for (int i = 0; i < SLOTS; i++) items.set(i, snapshot.get(i).copy());
        save();
    }

    public boolean add(ItemStack incoming) {
        if (incoming.isEmpty() || !canAdd(incoming)) return false;
        int remaining = incoming.getCount();
        for (ItemStack stack : items) {
            if (!ItemStack.isSameItemSameTags(stack, incoming)) continue;
            int given = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
            stack.grow(given);
            remaining -= given;
            if (remaining == 0) { save(); return true; }
        }
        for (int i = 0; i < SLOTS && remaining > 0; i++) {
            if (!items.get(i).isEmpty()) continue;
            ItemStack stack = incoming.copy();
            stack.setCount(Math.min(remaining, incoming.getMaxStackSize()));
            items.set(i, stack);
            remaining -= stack.getCount();
        }
        save();
        return remaining == 0;
    }

    public void save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Food", food);
        tag.putInt("Water", water);
        tag.putInt("Rest", rest);
        tag.putString("Doing", doing);
        ListTag history = new ListTag();
        for (String event : events) history.add(StringTag.valueOf(event));
        tag.put("Events", history);
        ListTag saved = new ListTag();
        for (int i = 0; i < SLOTS; i++) if (!items.get(i).isEmpty()) {
            CompoundTag row = new CompoundTag();
            row.putByte("Slot", (byte) i);
            items.get(i).save(row);
            saved.add(row);
        }
        tag.put("Items", saved);
        owner.getPersistentData().put(KEY, tag);
    }
}
