package com.bettercontent.spiritcommerce.spirit;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import com.bettercontent.spiritcommerce.registry.CurrencyItems;
import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.container.SpiritPouchContainer;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.registry.common.ContainerRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import java.util.UUID;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class SpiritPouchGameTests {
    private SpiritPouchGameTests() {}

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
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

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void creditedSpiritStackReleasesAsOneOwnedPickup(final GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.nameUUIDFromBytes("grouped-spirit-gametest".getBytes()), "grouped-spirit"));
        player.moveTo(helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getCenter());
        ItemStack credit = new ItemStack(CurrencyItems.item(CurrencyIdentity.WORK).get(), 4);
        SpiritAcquisition.releaseGroupedSpirits(List.of(credit), player, player);
        var spirits = helper.getLevel().getEntitiesOfClass(SpiritItemEntity.class,
                new AABB(player.blockPosition()).inflate(4));
        helper.assertTrue(spirits.size() == 1 && spirits.get(0).getItem().getCount() == 4,
                "One credited kill must release its full stack as one Malum spirit pickup; entities="
                        + spirits.size() + ", first stack=" + (spirits.isEmpty() ? "none" : spirits.get(0).getItem()));
        helper.succeed();
    }
}
