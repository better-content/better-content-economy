package com.bettercontent.economy.trader;

import com.bettercontent.economy.registry.SpiritProfessions;
import com.bettercontent.economy.spirit.SpiritKind;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.registry.CurrencyItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** One matching-spirit purchase grants two eggs locked to that spirit profession. */
public final class VillageStarterOffer {
    public static final int SPIRIT_COST = 2;
    public static final int VILLAGER_COUNT = 2;

    private VillageStarterOffer() {}

    public static void ensurePresent(final WanderingTrader trader, final MerchantOffers offers) {
        if (offers.stream().anyMatch(offer -> offer.getResult().is(Items.VILLAGER_SPAWN_EGG))) return;
        WanderingTraderTheme theme = WanderingTraderTheme.fromId(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG));
        if (theme == null) return;
        SpiritKind kind = theme.spirit();
        Item spirit = CurrencyItems.item(CurrencyIdentity.fromLegacyNativeSpirit(kind.itemId())).get();
        ItemStack eggs = new ItemStack(Items.VILLAGER_SPAWN_EGG, VILLAGER_COUNT);
        CompoundTag entity = eggs.getOrCreateTagElement("EntityTag");
        CompoundTag data = new CompoundTag();
        data.putString("type", "minecraft:plains");
        data.putString("profession", SpiritProfessions.definition(kind).profession().getId().toString());
        data.putInt("level", 1);
        entity.put("VillagerData", data);
        offers.add(new MerchantOffer(new ItemStack(spirit, SPIRIT_COST), eggs, 1, 0, 0.0F));
    }
}
