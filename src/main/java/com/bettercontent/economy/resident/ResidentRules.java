package com.bettercontent.economy.resident;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class ResidentRules {
    private static final ResourceLocation RAT = new ResourceLocation("rats", "rat");
    private static final String WORKSITE = "better_content_economy:worksite";
    private ResidentRules() {}

    public static boolean isResident(Entity entity) {
        if (entity instanceof Villager) return true;
        return entity instanceof LivingEntity && RAT.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
                && entity.getPersistentData().getBoolean("better_content_economy:resident_rat");
    }

    public static BlockPos worksite(LivingEntity resident) {
        return resident.getPersistentData().contains(WORKSITE)
                ? BlockPos.of(resident.getPersistentData().getLong(WORKSITE)) : resident.blockPosition();
    }

    public static boolean hasWorksite(LivingEntity resident) {
        return resident.getPersistentData().contains(WORKSITE);
    }

    public static void setWorksite(LivingEntity resident, BlockPos pos) {
        resident.getPersistentData().putLong(WORKSITE, pos.asLong());
    }

    public static ItemStack waterBottle(int purity) {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        stack.getOrCreateTag().putInt("Purity", purity);
        return stack;
    }

    public static boolean isSafeWater(ItemStack stack) {
        if (stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER)
            return stack.getTag() != null && stack.getTag().getInt("Purity") >= 3;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && (id.toString().equals("thirst:terracotta_water_bowl")
                && stack.getTag() != null && stack.getTag().getInt("Purity") >= 3
                || id.toString().startsWith("toughasnails:") && id.getPath().contains("purified"));
    }

    public static boolean hasShelter(ServerLevel level, BlockPos pos) {
        if (level.canSeeSky(pos.above())) return false;
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-12, -4, -12), pos.offset(12, 4, 12))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof BedBlock || state.getBlock().getDescriptionId().contains("rat_hole"))
                return true;
        }
        return false;
    }

    public static int basicValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (isSafeWater(stack)) return 4;
        if (stack.getFoodProperties(null) != null) return Math.max(1, stack.getFoodProperties(null).getNutrition());
        if (stack.is(net.minecraftforge.common.Tags.Items.GEMS)) return 16;
        if (stack.is(net.minecraftforge.common.Tags.Items.INGOTS)
                || stack.is(net.minecraftforge.common.Tags.Items.ORES)) return 8;
        if (stack.isDamageableItem())
            return Math.max(4, Math.min(32, stack.getMaxDamage() / 128));
        if (stack.is(Items.OAK_LOG) || stack.is(Items.SPRUCE_LOG) || stack.is(Items.BIRCH_LOG)) return 2;
        return 1;
    }

    public static boolean needs(ItemStack item, ResidentState state) {
        if (isSafeWater(item)) return state.water() < 14;
        if (item.getFoodProperties(null) != null) return state.food() < 14;
        if (item.is(net.minecraft.tags.ItemTags.LOGS)) return stock(state, net.minecraft.tags.ItemTags.LOGS) < 8;
        if (item.is(Items.COAL) || item.is(Items.CHARCOAL))
            return state.count(new ItemStack(Items.COAL)) + state.count(new ItemStack(Items.CHARCOAL)) < 4;
        if (item.is(Items.GLASS_BOTTLE)) return state.count(item) < 4;
        if (item.is(Blocks.COBBLESTONE.asItem())) return state.count(item) < 8;
        return false;
    }

    private static int stock(ResidentState state, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        int total = 0;
        for (ItemStack stack : state.items()) if (stack.is(tag)) total += stack.getCount();
        return total;
    }

    public static String needLabel(ResidentState state, boolean shelter) {
        if (state.water() < 8) return "Needs safe water";
        if (state.food() < 8) return "Needs food";
        if (!shelter || state.rest() < 8) return "Needs shelter and rest";
        return "Needs met";
    }
}
