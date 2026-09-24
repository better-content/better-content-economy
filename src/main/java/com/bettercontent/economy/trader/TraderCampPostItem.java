package com.bettercontent.economy.trader;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Carries the trader link while the player moves the campsite post. */
public final class TraderCampPostItem extends BlockItem {
    public TraderCampPostItem(final TraderCampPostBlock block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Level level,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        if (tag != null && tag.hasUUID(TraderCampPostBlockEntity.TRADER_TAG)) {
            tooltip.add(Component.translatable("tooltip.better_content_economy.trader_camp_post.linked")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
