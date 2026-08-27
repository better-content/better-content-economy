package com.bettercontent.economy.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

/** Replaces emerald currency emitted by an explicitly-owned loot table. */
public final class EmeraldReplacementLootModifier extends LootModifier {
    public static final Codec<EmeraldReplacementLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance)
                    .and(ResourceLocation.CODEC.fieldOf("coin").forGetter(modifier -> modifier.coin))
                    .and(Codec.INT.fieldOf("count").forGetter(modifier -> modifier.count))
                    .apply(instance, EmeraldReplacementLootModifier::new));

    private final ResourceLocation coin;
    private final int count;

    public EmeraldReplacementLootModifier(LootItemCondition[] conditions, ResourceLocation coin, int count) {
        super(conditions);
        this.coin = coin;
        this.count = count;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Item replacement = ForgeRegistries.ITEMS.getValue(coin);
        if (replacement == null || replacement == Items.AIR) return loot;
        for (int index = 0; index < loot.size(); index++) {
            if (loot.get(index).is(Items.EMERALD)) loot.set(index, new ItemStack(replacement, count));
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
