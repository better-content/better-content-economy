package com.bettercontent.economy.spirit;

import com.mojang.brigadier.CommandDispatcher;
import com.sammy.malum.common.container.SpiritPouchContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/** Opens Malum's existing pouch inventory only for the caller's equipped belt pouch. */
public final class SpiritPouchAccess {
    private static final ResourceLocation SPIRIT_POUCH = new ResourceLocation("malum", "spirit_pouch");

    private SpiritPouchAccess() {}

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spiritpouch").executes(context -> open(context.getSource().getPlayerOrException())));
    }

    private static int open(final ServerPlayer player) {
        final Item pouch = ForgeRegistries.ITEMS.getValue(SPIRIT_POUCH);
        if (pouch == null) {
            player.sendSystemMessage(Component.translatable("command.better_content_economy.spiritpouch.missing"));
            return 0;
        }
        final var equipped = CuriosApi.getCuriosHelper().findCurios(player, stack -> stack.is(pouch)).stream()
                .filter(result -> "belt".equals(result.slotContext().identifier())
                        && !result.slotContext().cosmetic() && result.slotContext().entity() == player)
                .findFirst();
        if (equipped.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.better_content_economy.spiritpouch.required"));
            return 0;
        }
        final ItemStack stack = equipped.get().stack();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new SpiritPouchContainer(containerId, inventory, stack),
                stack.getHoverName()), buffer -> buffer.writeItem(stack));
        return 1;
    }
}
