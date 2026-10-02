package com.bettercontent.spiritcommerce.resident;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PlayerStallRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BetterSpiritCommerce.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BetterSpiritCommerce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, BetterSpiritCommerce.MOD_ID);
    public static final RegistryObject<PlayerStallBlock> BLOCK = BLOCKS.register("player_stall", PlayerStallBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register("player_stall",
            () -> new BlockItem(BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<PlayerStallBlockEntity>> ENTITY = ENTITIES.register("player_stall",
            () -> BlockEntityType.Builder.of(PlayerStallBlockEntity::new, BLOCK.get()).build(null));

    private PlayerStallRegistries() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
    }
}
