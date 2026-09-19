package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.spirit.SpiritKind;
import com.bettercontent.economy.registry.CurrencyItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Eight fixed wandering markets. Each visit exposes thirteen goods plus its profession egg. */
public final class WanderingTraderCatalogue {
    private WanderingTraderCatalogue() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(final WandererTradesEvent event) {
        event.getGenericTrades().clear();
        event.getRareTrades().clear();
        for (int index = 0; index < 5; index++) event.getGenericTrades().add(new BootstrapListing());
        event.getRareTrades().add(new BootstrapListing());
    }

    public static void ensureThemedOffers(final WanderingTrader trader, final MerchantOffers offers) {
        WanderingTraderTheme theme = WanderingTraderTheme.fromId(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG));
        if (theme == null) theme = WanderingTraderTheme.forUuid(trader.getUUID());
        if (offers.size() == 14 && offers.stream().anyMatch(offer -> offer.getResult().is(net.minecraft.world.item.Items.VILLAGER_SPAWN_EGG))) return;
        offers.clear();
        SpiritKind spirit = SpiritKind.fromId(theme.id());
        Item payment = CurrencyItems.item(theme.currencyIdentity()).get();
        for (EconomyPolicy.WanderingRow row : EconomyPolicy.wanderingRows(spirit)) {
            Item result = item(row.result().id());
            if (payment != null && result != null) offers.add(new MerchantOffer(
                    new ItemStack(payment, row.cost()), new ItemStack(result, row.result().count()),
                    row.maxUses(), row.xp(), 0.0F));
        }
    }

    public static int themeCount() { return WanderingTraderTheme.values().length; }
    public static int authoredRowCount() { return EconomyPolicy.wanderingRowCount(); }

    private static Item item(final String id) { return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)); }
    private record BootstrapListing() implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(final Entity entity, final RandomSource random) {
            WanderingTraderTheme theme = entity instanceof WanderingTrader trader
                    ? WanderingTraderTheme.fromId(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG)) : null;
            if (theme == null && entity != null) theme = WanderingTraderTheme.forUuid(entity.getUUID());
            if (theme == null) theme = WanderingTraderTheme.SACRED;
            Item payment = CurrencyItems.item(theme.currencyIdentity()).get();
            return payment == null ? null : new MerchantOffer(new ItemStack(payment),
                    new ItemStack(net.minecraft.world.item.Items.BREAD), 1, 0, 0.0F);
        }
    }
}
