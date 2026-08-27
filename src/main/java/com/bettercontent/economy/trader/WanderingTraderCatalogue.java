package com.bettercontent.economy.trader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** The authoritative coin-only wandering-trader catalogue. */
public final class WanderingTraderCatalogue {
    private static final Map<String, String> COINS = Map.of(
            "copper", "createdeco:copper_coin", "zinc", "createdeco:zinc_coin",
            "iron", "createdeco:iron_coin", "brass", "createdeco:brass_coin",
            "gold", "createdeco:gold_coin", "platinum", "createdeco:netherite_coin");
    private static final EnumMap<WanderingTraderTheme, Market> MARKETS = new EnumMap<>(WanderingTraderTheme.class);

    static {
        market(WanderingTraderTheme.NATURALIST,
                slot("copper,2,minecraft:oak_sapling,4,8,2;copper,3,minecraft:jungle_sapling,4,8,2;copper,4,minecraft:mangrove_propagule,2,6,3;copper,4,minecraft:cherry_sapling,2,6,3"),
                slot("copper,2,minecraft:sugar_cane,8,8,2;copper,3,minecraft:bamboo,16,8,2;copper,2,minecraft:brown_mushroom,8,8,2;copper,4,minecraft:glow_berries,8,6,3"),
                slot("copper,3,minecraft:blue_orchid,4,8,2;copper,3,minecraft:cornflower,4,8,2;copper,4,minecraft:spore_blossom,2,4,6;copper,3,minecraft:pink_petals,8,6,4"),
                slot("copper,3,minecraft:moss_block,8,8,2;copper,4,minecraft:big_dripleaf,2,6,3;copper,3,minecraft:sea_pickle,4,8,2;zinc,4,minecraft:mycelium,8,6,4"),
                slot("iron,4,minecraft:honeycomb,8,8,8;iron,4,minecraft:honey_bottle,4,8,8;iron,5,minecraft:slime_ball,8,8,8;iron,5,minecraft:rabbit_foot,2,6,8"),
                slot("brass,6,minecraft:cod_bucket,1,3,12;gold,6,minecraft:axolotl_bucket,1,2,16;gold,6,minecraft:torchflower_seeds,2,3,18;gold,7,minecraft:sniffer_egg,1,1,22"));
        market(WanderingTraderTheme.SURVEYOR,
                slot("zinc,2,minecraft:clay,8,8,4;zinc,3,minecraft:mud,16,8,4;zinc,3,minecraft:terracotta,16,8,4;zinc,3,minecraft:red_sand,24,8,4"),
                slot("zinc,3,minecraft:calcite,16,8,4;zinc,3,minecraft:tuff,24,8,4;zinc,3,minecraft:dripstone_block,16,8,4;iron,5,minecraft:blue_ice,8,4,8"),
                slot("zinc,3,minecraft:lantern,4,8,4;zinc,4,minecraft:soul_lantern,4,8,4;zinc,4,minecraft:campfire,2,8,4;zinc,5,minecraft:soul_campfire,2,6,6"),
                slot("iron,5,minecraft:scaffolding,32,8,8;iron,4,minecraft:chain,16,8,8;iron,6,minecraft:lead,4,6,8;brass,6,create:super_glue,1,4,14"),
                slot("iron,5,create:track,32,5,12;brass,5,minecraft:compass,1,4,12;gold,5,minecraft:spyglass,1,3,16;brass,5,minecraft:clock,1,4,12"),
                slot("gold,6,minecraft:recovery_compass,1,2,18;gold,7,create:linked_controller,1,2,18;gold,6,additionalweaponry:wrench,1,3,16;gold,6,create:copper_diving_helmet,1,2,16"));
        market(WanderingTraderTheme.QUARTERMASTER,
                slot("iron,6,minecraft:saddle,1,4,8;iron,6,minecraft:lead,4,6,8;iron,5,minecraft:name_tag,1,4,8;iron,5,minecraft:bell,1,4,8"),
                slot("brass,5,minecraft:ender_pearl,2,6,12;brass,5,minecraft:magma_cream,4,6,12;brass,4,minecraft:glowstone_dust,12,8,12;brass,4,minecraft:quartz,16,8,12"),
                slot("brass,5,minecraft:clock,1,4,12;brass,5,minecraft:compass,1,4,12;gold,5,minecraft:spyglass,1,3,16;gold,6,minecraft:recovery_compass,1,2,18"),
                slot("brass,6,create:super_glue,1,4,14;gold,6,create:copper_diving_boots,1,2,16;gold,6,create:copper_diving_helmet,1,2,16;gold,6,additionalweaponry:wrench,1,3,16"),
                slot("gold,5,minecraft:blaze_rod,4,4,16;gold,5,minecraft:ghast_tear,2,4,16;gold,6,minecraft:phantom_membrane,2,4,18;gold,6,minecraft:echo_shard,1,3,18"),
                slot("platinum,8,pneumaticcraft:night_vision_upgrade,1,1,24;platinum,8,minecraft:shulker_shell,1,2,22;platinum,7,minecraft:totem_of_undying,1,1,24;gold,6,minecraft:heart_of_the_sea,1,2,18"));
        market(WanderingTraderTheme.ANTIQUARIAN,
                slot("brass,5,minecraft:music_disc_13,1,2,12;brass,5,minecraft:music_disc_cat,1,2,12;gold,5,minecraft:music_disc_otherside,1,2,16;gold,5,minecraft:music_disc_5,1,2,16"),
                slot("gold,6,minecraft:angler_pottery_sherd,1,2,18;gold,6,minecraft:archer_pottery_sherd,1,2,18;gold,6,minecraft:brewer_pottery_sherd,1,2,18;gold,6,minecraft:miner_pottery_sherd,1,2,18"),
                slot("platinum,7,minecraft:coast_armor_trim_smithing_template,1,1,22;platinum,7,minecraft:dune_armor_trim_smithing_template,1,1,22;platinum,7,minecraft:ward_armor_trim_smithing_template,1,1,22;platinum,7,minecraft:wild_armor_trim_smithing_template,1,1,22"),
                slot("gold,6,minecraft:torchflower_seeds,2,3,18;gold,6,minecraft:pitcher_pod,2,3,18;gold,6,minecraft:echo_shard,1,3,18;brass,5,minecraft:amethyst_shard,12,8,12"),
                slot("iron,5,minecraft:bell,1,4,8;gold,5,minecraft:spyglass,1,3,16;brass,5,minecraft:nautilus_shell,2,4,12;gold,6,minecraft:heart_of_the_sea,1,2,18"),
                slot("platinum,8,minecraft:dragon_head,1,1,28;platinum,7,minecraft:silence_armor_trim_smithing_template,1,1,24;platinum,7,minecraft:spire_armor_trim_smithing_template,1,1,22;gold,7,minecraft:sniffer_egg,1,1,22"));
    }

    private WanderingTraderCatalogue() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(final WandererTradesEvent event) {
        VillagerTrades.ItemListing regularAgreement = findAgreement(event.getGenericTrades());
        VillagerTrades.ItemListing rareAgreement = findAgreement(event.getRareTrades());
        event.getGenericTrades().clear();
        event.getRareTrades().clear();
        for (int slot = 0; slot < 5; slot++) event.getGenericTrades().add(new ThemedListing(slot, false));
        event.getRareTrades().add(new ThemedListing(0, true));
        if (regularAgreement != null || rareAgreement != null) {
            event.getGenericTrades().add(new AgreementListing(regularAgreement, rareAgreement));
        }
        optionalFontMap().ifPresent(event.getGenericTrades()::add);
    }

    static int themeCount() { return MARKETS.size(); }
    static int authoredRowCount() {
        return MARKETS.values().stream().mapToInt(m -> m.common().stream().mapToInt(List::size).sum() + m.rare().size()).sum();
    }

    private static void market(WanderingTraderTheme theme, List<Row> a, List<Row> b, List<Row> c, List<Row> d, List<Row> e, List<Row> rare) {
        MARKETS.put(theme, new Market(List.of(a, b, c, d, e), rare));
    }

    private static List<Row> slot(String encoded) {
        List<Row> rows = new ArrayList<>();
        for (String entry : encoded.split(";")) {
            String[] p = entry.split(",");
            rows.add(new Row(p[0], Integer.parseInt(p[1]), p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])));
        }
        return List.copyOf(rows);
    }

    private static java.util.Optional<VillagerTrades.ItemListing> optionalFontMap() {
        try {
            Class<?> type = Class.forName("com.bettercontent.dimensiondrink.trade.DimensionalFontMapTrades");
            Method method = type.getMethod("wanderingTraderListing", int.class);
            return java.util.Optional.of((VillagerTrades.ItemListing) method.invoke(null, 0));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return java.util.Optional.empty();
        }
    }

    private static VillagerTrades.ItemListing findAgreement(List<VillagerTrades.ItemListing> listings) {
        RandomSource random = RandomSource.create(1L);
        for (VillagerTrades.ItemListing listing : listings) {
            try {
                MerchantOffer offer = listing.getOffer(null, random);
                ResourceLocation id = offer == null ? null : ForgeRegistries.ITEMS.getKey(offer.getResult().getItem());
                if (new ResourceLocation("wares", "sealed_delivery_agreement").equals(id)) return listing;
            } catch (RuntimeException ignored) {
                // Optional listing requires a live trader; it is not the Wares agreement adapter we need.
            }
        }
        return null;
    }

    private record Market(List<List<Row>> common, List<Row> rare) {}
    private record Row(String coin, int cost, String output, int count, int maxUses, int xp) {
        MerchantOffer offer() {
            Item payment = ForgeRegistries.ITEMS.getValue(new ResourceLocation(COINS.get(coin)));
            Item result = ForgeRegistries.ITEMS.getValue(new ResourceLocation(output));
            if (payment == null || result == null) return null;
            return new MerchantOffer(new ItemStack(payment, cost), new ItemStack(result, count), maxUses, xp, 0.0F);
        }
    }

    private record ThemedListing(int slot, boolean rare) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(Entity entity, RandomSource random) {
            WanderingTraderTheme theme = entity instanceof WanderingTrader trader
                    ? WanderingTraderTheme.fromId(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG)) : null;
            if (theme == null && entity != null) theme = WanderingTraderTheme.forUuid(entity.getUUID());
            if (theme == null) theme = WanderingTraderTheme.NATURALIST;
            Market market = MARKETS.get(theme);
            List<Row> pool = rare ? market.rare() : market.common().get(slot);
            int start = random.nextInt(pool.size());
            for (int offset = 0; offset < pool.size(); offset++) {
                MerchantOffer offer = pool.get((start + offset) % pool.size()).offer();
                if (offer != null) return offer;
            }
            Item copper = ForgeRegistries.ITEMS.getValue(new ResourceLocation(COINS.get("copper")));
            Item torch = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", "torch"));
            return new MerchantOffer(new ItemStack(copper, 1), new ItemStack(torch, 4), 1, 0, 0.0F);
        }
    }

    private record AgreementListing(VillagerTrades.ItemListing regular, VillagerTrades.ItemListing rare)
            implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(Entity entity, RandomSource random) {
            boolean antiquarian = entity instanceof WanderingTrader trader
                    && WanderingTraderTheme.ANTIQUARIAN == WanderingTraderTheme.fromId(
                            trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG));
            VillagerTrades.ItemListing selected = antiquarian && rare != null ? rare : regular != null ? regular : rare;
            MerchantOffer source = selected == null ? null : selected.getOffer(entity, random);
            Item copper = ForgeRegistries.ITEMS.getValue(new ResourceLocation(COINS.get("copper")));
            return source == null || copper == null ? null
                    : new MerchantOffer(new ItemStack(copper, antiquarian ? 12 : 6), source.getResult().copy(), 1, 0, 0.0F);
        }
    }
}
