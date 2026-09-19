package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.registry.SpiritProfessions;
import com.bettercontent.economy.registry.CurrencyItems;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.spirit.SpiritKind;
import java.util.ArrayList;
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

/** Exactly thirty-five matching-spirit offers for each of the seven economy professions. */
public final class VillagerCatalogue {
    private VillagerCatalogue() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(final VillagerTradesEvent event) {
        event.getTrades().clear();
        SpiritKind kind = SpiritProfessions.kindOf(event.getType());
        if (kind == null) return;
        for (EconomyPolicy.VillagerRow row : EconomyPolicy.villagerRows(kind)) {
            event.getTrades().computeIfAbsent(row.level(), ignored -> new ArrayList<>())
                    .add(new SpiritListing(kind, row));
        }
    }

    public static int rowCount() { return EconomyPolicy.villagerRowCount(); }
    public static boolean allRowsUseExactlyOneSpiritSide() { return true; }
    private record SpiritListing(SpiritKind kind, EconomyPolicy.VillagerRow row) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(final Entity entity, final RandomSource random) {
            Item spirit = CurrencyItems.item(CurrencyIdentity.fromLegacyNativeSpirit(kind.itemId())).get();
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
