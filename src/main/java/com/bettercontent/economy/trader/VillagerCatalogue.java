package com.bettercontent.economy.trader;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Exact coin-only village catalogue extracted from the former pack script. */
public final class VillagerCatalogue {
    private static final Catalogue CATALOGUE = load();

    private VillagerCatalogue() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(VillagerTradesEvent event) {
        ResourceLocation professionId = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(event.getType());
        if (professionId == null) return;
        event.getTrades().clear();
        for (Row row : CATALOGUE.rows) {
            if (!professionId.toString().equals(row.profession) || row.level < 1 || row.level > 5) continue;
            if (!row.resolves()) continue;
            event.getTrades().computeIfAbsent(row.level, ignored -> new ArrayList<>()).add(new FixedListing(row));
        }
    }

    static int rowCount() { return CATALOGUE.rows.size(); }

    static boolean allRowsUseExactlyOneCoinSide() {
        return CATALOGUE.rows.stream().allMatch(row -> isCoin(row.input.id) ^ isCoin(row.result.id));
    }

    private static boolean isCoin(String id) {
        return id.startsWith("createdeco:") && (id.endsWith("_coin") || id.endsWith("_coinstack"));
    }

    private static Catalogue load() {
        var stream = VillagerCatalogue.class.getResourceAsStream(
                "/data/better_content_economy/economy/villager_catalogue.json");
        try (var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            Catalogue catalogue = new Gson().fromJson(reader, Catalogue.class);
            if (catalogue == null || catalogue.rows == null) throw new IllegalStateException("Missing villager catalogue rows");
            return catalogue;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load Better Content Economy villager catalogue", exception);
        }
    }

    private static final class Catalogue { List<Row> rows; }
    private static final class StackSpec { String id; int count; }
    private static final class Row {
        String profession;
        int level;
        StackSpec input;
        StackSpec result;
        int maxUses;
        int xp;

        boolean resolves() { return item(input.id) != Items.AIR && item(result.id) != Items.AIR; }
        MerchantOffer offer() {
            return new MerchantOffer(
                    new ItemStack(item(input.id), input.count),
                    new ItemStack(item(result.id), result.count),
                    maxUses,
                    xp,
                    0.0F);
        }
    }

    private record FixedListing(Row row) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(Entity entity, RandomSource random) { return row.offer(); }
    }

    private static Item item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? Items.AIR : item;
    }
}
