package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.registry.CurrencyItems;
import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.container.SpiritPouchContainer;
import com.sammy.malum.registry.common.ContainerRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class SpiritPouchGameTests {
    private SpiritPouchGameTests() {}

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty")
    public static void pouchMenuAcceptsSpiritsAndRejectsOtherItems(final GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.nameUUIDFromBytes("spirit-pouch-gametest".getBytes()), "spirit-pouch-test"));
        SpiritPouchContainer menu = new SpiritPouchContainer(ContainerRegistry.SPIRIT_POUCH.get(), 1,
                player.getInventory(), new SimpleContainer(9));
        var pouchSlot = menu.slots.get(0);
        helper.assertTrue(pouchSlot.mayPlace(new ItemStack(CurrencyItems.item(CurrencyIdentity.WORK).get())),
                "Pouch must accept economy currency");
        helper.assertTrue(pouchSlot.mayPlace(new ItemStack(ItemRegistry.ELDRITCH_SPIRIT.get())),
                "Pouch must keep accepting native Malum spirits");
        helper.assertTrue(!pouchSlot.mayPlace(new ItemStack(Items.DIRT)),
                "Pouch must reject ordinary items");
        menu.removed(player);
        helper.succeed();
    }
}
