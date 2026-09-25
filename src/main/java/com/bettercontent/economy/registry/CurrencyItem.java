package com.bettercontent.economy.registry;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.spirit.EconomySpiritTypes;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Adds a text identity cue that remains usable without color or texture recognition. */
final class CurrencyItem extends SpiritShardItem {
    private final CurrencyIdentity identity;
    CurrencyItem(CurrencyIdentity identity) {
        super(new Item.Properties().stacksTo(64), EconomySpiritTypes.type(identity));
        this.identity = identity;
    }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                                           List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (identity != CurrencyIdentity.TEMPO) super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.better_content_economy." + identity.id()));
    }
}
