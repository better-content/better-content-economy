package com.bettercontent.economy.registry;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Adds a text identity cue that remains usable without color or texture recognition. */
final class CurrencyItem extends Item {
    private final CurrencyIdentity identity;
    CurrencyItem(CurrencyIdentity identity) { super(new Item.Properties()); this.identity = identity; }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                                           List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.better_content_economy.currency_identity", identity.id()));
    }
}
