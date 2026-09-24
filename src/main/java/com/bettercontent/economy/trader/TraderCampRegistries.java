package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registry entries for the movable campsite marker and its rendered awning. */
public final class TraderCampRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, BetterContentEconomy.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, BetterContentEconomy.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, BetterContentEconomy.MOD_ID);

    public static final RegistryObject<TraderCampPostBlock> POST = BLOCKS.register(
            "trader_camp_post", TraderCampPostBlock::new);
    public static final RegistryObject<Item> POST_ITEM = ITEMS.register(
            "trader_camp_post", () -> new TraderCampPostItem(POST.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<TraderCampPostBlockEntity>> POST_ENTITY = BLOCK_ENTITIES.register(
            "trader_camp_post", () -> BlockEntityType.Builder.of(
                    TraderCampPostBlockEntity::new, POST.get()).build(null));

    private TraderCampRegistries() {
    }

    public static void register(final IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
