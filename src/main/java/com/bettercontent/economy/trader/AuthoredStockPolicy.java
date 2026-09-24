package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.spirit.SpiritKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.trading.MerchantOffer;

/** One-time, catalogue-bounded stock for high-value direct-sale commodities. */
public final class AuthoredStockPolicy {
    private static final Set<ResourceLocation> HIGH_VALUE_CANDIDATES = Set.of(
            id("diamond"), id("diamond_boots"), id("diamond_helmet"), id("diamond_chestplate"),
            id("diamond_leggings"), id("diamond_sword"), id("diamond_axe"), id("diamond_pickaxe"),
            id("diamond_shovel"), id("diamond_hoe"), id("netherite_scrap"), id("netherite_ingot"),
            id("netherite_boots"), id("netherite_helmet"), id("netherite_chestplate"),
            id("netherite_leggings"), id("netherite_sword"), id("netherite_axe"),
            id("netherite_pickaxe"), id("netherite_shovel"), id("netherite_hoe"), id("elytra"));
    private static final List<StockOffer> NATIVE_DIRECT_SALE_ROWS = List.of(
            row("diamond_leggings", 3), row("diamond_boots", 3),
            row("diamond_helmet", 3), row("diamond_chestplate", 3),
            row("diamond_axe", 3), row("diamond_shovel", 3),
            row("diamond_pickaxe", 3), row("diamond_hoe", 3),
            row("diamond_axe", 3), row("diamond_sword", 3),
            // Netherite is an authored high-value outlet too; one bounded seed row
            // per exact output keeps all sellers on the same finite commodity pool.
            row("netherite_ingot", 1), row("netherite_boots", 1),
            row("netherite_helmet", 1), row("netherite_chestplate", 1),
            row("netherite_leggings", 1), row("netherite_sword", 1),
            row("netherite_axe", 1), row("netherite_pickaxe", 1),
            row("netherite_shovel", 1), row("netherite_hoe", 1));
    private static final List<StockOffer> AUTHORED_ECONOMY_ROWS = authoredEconomyRows();
    private static final Map<String, Integer> INITIAL_STOCK_UNITS = buildInitialStockUnits();
    private static final Set<ResourceLocation> FINITE_GOODS = INITIAL_STOCK_UNITS.keySet().stream()
            .map(ResourceLocation::new).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private AuthoredStockPolicy() {}

    private static ResourceLocation id(final String path) {
        return new ResourceLocation("minecraft", path);
    }

    public static boolean isFinite(final MerchantOffer offer) {
        ResourceLocation result = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(offer.getResult().getItem());
        return isFinite(result);
    }

    static boolean isFinite(final ResourceLocation result) {
        return result != null && FINITE_GOODS.contains(result);
    }

    static Map<String, Integer> initialStockUnits() {
        return INITIAL_STOCK_UNITS;
    }

    static int seedSourceRowCount() {
        return NATIVE_DIRECT_SALE_ROWS.size()
                + (int) AUTHORED_ECONOMY_ROWS.stream()
                        .filter(offer -> HIGH_VALUE_CANDIDATES.contains(offer.result())
                                && offer.count() > 0 && offer.maxUses() > 0)
                        .count();
    }

    private static Map<String, Integer> buildInitialStockUnits() {
        Map<String, Integer> stock = new HashMap<>();
        NATIVE_DIRECT_SALE_ROWS.forEach(offer -> addOffer(stock, offer));
        AUTHORED_ECONOMY_ROWS.forEach(offer -> addOffer(stock, offer));
        return Map.copyOf(stock);
    }

    private static List<StockOffer> authoredEconomyRows() {
        List<StockOffer> offers = new ArrayList<>();
        for (SpiritKind kind : SpiritKind.values()) {
            for (EconomyPolicy.VillagerRow row : EconomyPolicy.villagerRows(kind)) {
                offers.add(new StockOffer(new ResourceLocation(row.result().id()), row.result().count(), row.maxUses()));
            }
            for (EconomyPolicy.WanderingRow row : EconomyPolicy.wanderingRows(kind)) {
                offers.add(new StockOffer(new ResourceLocation(row.result().id()), row.result().count(), row.maxUses()));
            }
        }
        return List.copyOf(offers);
    }

    private static void addOffer(final Map<String, Integer> stock, final StockOffer offer) {
        if (offer == null || !HIGH_VALUE_CANDIDATES.contains(offer.result())
                || offer.count() <= 0 || offer.maxUses() <= 0) return;
        stock.merge(AuthoredOfferStock.key(offer.result()), Math.multiplyExact(offer.count(), offer.maxUses()), Math::addExact);
    }

    static Map<String, Integer> aggregate(final List<StockOffer> offers) {
        Map<String, Integer> stock = new HashMap<>();
        offers.forEach(offer -> addOffer(stock, offer));
        return Map.copyOf(stock);
    }

    private static StockOffer row(final String result, final int maxUses) {
        return new StockOffer(id(result), 1, maxUses);
    }

    record StockOffer(ResourceLocation result, int count, int maxUses) {}
}
