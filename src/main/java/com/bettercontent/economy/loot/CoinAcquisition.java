package com.bettercontent.economy.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Authoritative non-crafting coin acquisition and chest-tier policy. */
public final class CoinAcquisition {
    private static final Map<ResourceLocation, List<Addition>> CHEST_ADDITIONS = buildChestAdditions();

    private CoinAcquisition() {}

    @SubscribeEvent
    public static void addCombatIncome(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player || !(event.getSource().getEntity() instanceof Player)) return;
        Item copper = item("createdeco:copper_coin");
        if (copper != Items.AIR) event.getEntity().spawnAtLocation(new ItemStack(copper));
    }

    @SubscribeEvent
    public static void addChestIncome(LootTableLoadEvent event) {
        List<Addition> additions = CHEST_ADDITIONS.get(event.getName());
        if (additions == null) return;
        int index = 0;
        for (Addition addition : additions) {
            Item coin = item(addition.coin());
            if (coin == Items.AIR) continue;
            LootPool pool = LootPool.lootPool()
                    .name("better_content_economy_coin_" + index++)
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(addition.chance()))
                    .add(LootItem.lootTableItem(coin)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(addition.count()))))
                    .build();
            event.getTable().addPool(pool);
        }
    }

    static int chestTableCount() { return CHEST_ADDITIONS.size(); }

    private static Map<ResourceLocation, List<Addition>> buildChestAdditions() {
        Map<ResourceLocation, List<Addition>> result = new LinkedHashMap<>();
        tier(result, "createdeco:copper_coin", 4, 0.55F,
                "minecraft:chests/spawn_bonus_chest,minecraft:chests/village/village_plains_house,minecraft:chests/village/village_desert_house,minecraft:chests/village/village_taiga_house,minecraft:chests/village/village_savanna_house,minecraft:chests/village/village_snowy_house,minecraft:chests/igloo_chest,minecraft:chests/underwater_ruin_small");
        tier(result, "createdeco:zinc_coin", 3, 0.45F,
                "minecraft:chests/shipwreck_supply,minecraft:chests/shipwreck_map,minecraft:chests/underwater_ruin_big,minecraft:chests/ruined_portal");
        tier(result, "createdeco:iron_coin", 3, 0.50F,
                "minecraft:chests/simple_dungeon,minecraft:chests/abandoned_mineshaft,minecraft:chests/pillager_outpost,minecraft:chests/desert_pyramid,minecraft:chests/jungle_temple");
        tier(result, "createdeco:industrial_iron_coin", 3, 0.40F,
                "minecraft:chests/buried_treasure,minecraft:chests/shipwreck_treasure,minecraft:chests/stronghold_corridor,minecraft:chests/stronghold_crossing");
        tier(result, "createdeco:brass_coin", 2, 0.30F,
                "minecraft:chests/woodland_mansion,minecraft:chests/ancient_city_ice_box,minecraft:chests/nether_bridge,minecraft:chests/bastion_bridge,minecraft:chests/bastion_hoglin_stable");
        tier(result, "createdeco:gold_coin", 2, 0.08F,
                "minecraft:chests/stronghold_library,minecraft:chests/ancient_city,minecraft:chests/bastion_other");
        tier(result, "createdeco:netherite_coin", 1, 0.001F,
                "minecraft:chests/bastion_treasure,minecraft:chests/end_city_treasure");
        return Map.copyOf(result);
    }

    private static void tier(Map<ResourceLocation, List<Addition>> result, String coin, int count, float chance, String tables) {
        for (String table : tables.split(",")) {
            List<Addition> additions = result.computeIfAbsent(new ResourceLocation(table), ignored -> new ArrayList<>());
            if (additions.isEmpty()) {
                additions.add(new Addition("createdeco:copper_coin", 4, 1.0F));
                additions.add(new Addition("createdeco:zinc_coin", 2, 0.9F));
                additions.add(new Addition("createdeco:iron_coin", 2, 0.85F));
            }
            additions.add(new Addition(coin, count, chance));
        }
    }

    private static Item item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? Items.AIR : item;
    }

    private record Addition(String coin, int count, float chance) {}
}
