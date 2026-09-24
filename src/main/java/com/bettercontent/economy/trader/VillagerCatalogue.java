package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.registry.SpiritProfessions;
import com.bettercontent.economy.registry.CurrencyItems;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.spirit.SpiritKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/** Exactly thirty-five matching-spirit offers for each ordinary economy profession, plus Tempo's authored set. */
public final class VillagerCatalogue {
    private VillagerCatalogue() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(final VillagerTradesEvent event) {
        SpiritKind kind = SpiritProfessions.kindOf(event.getType());
        replaceListings(kind, event.getTrades());
    }

    static boolean replaceListings(
            final SpiritKind kind,
            final Map<Integer, List<VillagerTrades.ItemListing>> trades) {
        if (kind == null) return false;
        trades.clear();
        for (EconomyPolicy.VillagerRow row : EconomyPolicy.villagerRows(kind)) {
            trades.computeIfAbsent(row.level(), ignored -> new ArrayList<>())
                    .add(new SpiritListing(kind, row));
        }
        return true;
    }

    public static int rowCount() { return EconomyPolicy.villagerRowCount(); }
    public static boolean allRowsUseExactlyOneSpiritSide() { return true; }
    private record SpiritListing(SpiritKind kind, EconomyPolicy.VillagerRow row) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(final Entity entity, final RandomSource random) {
            Item spirit = CurrencyItems.item(kind.currencyIdentity()).get();
            Item result = item(row.result().id());
            if (spirit == Items.AIR || result == Items.AIR) return null;
            return new MerchantOffer(new ItemStack(spirit, row.cost()),
                    new ItemStack(result, row.result().count()), row.maxUses(), row.xp(), 0.0F);
        }
    }

    private static Item item(final String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? Items.AIR : item;
    }
}
