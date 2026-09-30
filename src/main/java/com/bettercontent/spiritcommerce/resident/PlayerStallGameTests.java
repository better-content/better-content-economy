package com.bettercontent.spiritcommerce.resident;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class PlayerStallGameTests {
    private PlayerStallGameTests() {}

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void exactOfferTransfersPhysicalGoods(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        Villager buyer = buyer(helper);
        stall.seedStock(new ItemStack(Items.CARROT, 8));
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.OAK_LOG, 3), true);
        ResidentState.of(buyer).add(new ItemStack(Items.OAK_LOG, 8));
        helper.assertTrue(PlayerStallTrade.quote(helper.getLevel(), stall, buyer, 0).available(),
                "Useful exact offer should be affordable");
        helper.assertTrue(PlayerStallTrade.execute(helper.getLevel(), stall, buyer, 0)
                        && stall.stockCount(new ItemStack(Items.CARROT)) == 6
                        && PlayerStallBlockEntity.count(stall.proceeds(), new ItemStack(Items.OAK_LOG)) == 3
                        && ResidentState.of(buyer).count(new ItemStack(Items.OAK_LOG)) == 5
                        && ResidentState.of(buyer).count(new ItemStack(Items.CARROT)) == 2,
                "One sale must transfer exact counts and conserve all items");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void villagerCanCraftStallPaymentAtTradeTime(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.FURNACE);
        Villager buyer = buyer(helper);
        stall.seedStock(new ItemStack(Items.CARROT, 2));
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.CHARCOAL), true);
        ResidentState.of(buyer).add(new ItemStack(Items.OAK_LOG, 6));
        PlayerStallTrade.Quote quote = PlayerStallTrade.quote(helper.getLevel(), stall, buyer, 0);
        helper.assertTrue(quote.available() && quote.crafted(), "Furnace and logs should fund lazy payment");
        helper.assertTrue(PlayerStallTrade.execute(helper.getLevel(), stall, buyer, 0)
                        && PlayerStallBlockEntity.count(stall.proceeds(), new ItemStack(Items.CHARCOAL)) == 1
                        && ResidentState.of(buyer).count(new ItemStack(Items.OAK_LOG)) == 4,
                "Crafted payment must consume one input log and one fuel log");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void overpriceAndEmptyStockNeverChargeBuyer(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        Villager buyer = buyer(helper);
        ResidentState.of(buyer).add(new ItemStack(Items.OAK_LOG, 8));
        stall.seedStock(new ItemStack(Items.CARROT));
        stall.configure(0, new ItemStack(Items.CARROT), new ItemStack(Items.OAK_LOG, 3), true);
        helper.assertTrue(!PlayerStallTrade.quote(helper.getLevel(), stall, buyer, 0).available()
                        && !PlayerStallTrade.execute(helper.getLevel(), stall, buyer, 0),
                "High asking price must be rejected");
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.OAK_LOG, 3), true);
        helper.assertTrue(!PlayerStallTrade.quote(helper.getLevel(), stall, buyer, 0).available()
                        && ResidentState.of(buyer).count(new ItemStack(Items.OAK_LOG)) == 8
                        && stall.proceeds().stream().allMatch(ItemStack::isEmpty),
                "Out-of-stock listing must not take payment");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void fullProceedsBlocksTradeAndStateReloads(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        Villager buyer = buyer(helper);
        stall.seedStock(new ItemStack(Items.CARROT, 8));
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.OAK_LOG, 3), true);
        ResidentState.of(buyer).add(new ItemStack(Items.OAK_LOG, 8));
        for (int i = 0; i < PlayerStallBlockEntity.SLOTS; i++)
            helper.assertTrue(stall.seedProceeds(new ItemStack(Items.OAK_LOG, 64)), "Fixture must fill earnings");
        helper.assertTrue(!PlayerStallTrade.quote(helper.getLevel(), stall, buyer, 0).available(),
                "Full earnings must block the purchase");
        var saved = stall.saveWithFullMetadata();
        PlayerStallBlockEntity loaded = new PlayerStallBlockEntity(stall.getBlockPos(), stall.getBlockState());
        loaded.load(saved);
        helper.assertTrue(loaded.owner().equals(stall.owner())
                        && loaded.stockCount(new ItemStack(Items.CARROT)) == 8
                        && loaded.offers().get(0).payment().getCount() == 3
                        && !loaded.canReceive(new ItemStack(Items.OAK_LOG)),
                "Owner, inventory, and exact offer must persist");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void arrivalAndOwnershipAreRequired(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        ServerPlayer other = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.fromString("bce7a87b-4922-4cf0-b6a9-6a79bb993845"), "other_stall_owner"));
        helper.assertTrue(!stall.owns(other), "Another player cannot own the placed stall");
        Villager buyer = buyer(helper);
        buyer.setNoAi(true);
        buyer.moveTo(helper.absolutePos(new BlockPos(9, 2, 2)), 0, 0);
        stall.seedStock(new ItemStack(Items.CARROT, 8));
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.OAK_LOG, 3), true);
        ResidentState.of(buyer).add(new ItemStack(Items.OAK_LOG, 8));
        var level = helper.getLevel();
        PlayerStallBlockEntity.tick(level, stall.getBlockPos(), stall.getBlockState(), stall);
        helper.assertTrue(stall.visiting() && stall.proceeds().stream().allMatch(ItemStack::isEmpty),
                "A distant villager cannot trade before reaching the stall");
        buyer.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0, 0);
        PlayerStallBlockEntity.tick(level, stall.getBlockPos(), stall.getBlockState(), stall);
        helper.assertTrue(!stall.visiting()
                        && PlayerStallBlockEntity.count(stall.proceeds(), new ItemStack(Items.OAK_LOG)) == 3,
                "The reserved buyer must trade only after reaching the stall front");
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterSpiritCommerce.MOD_ID, template = "empty")
    public static void ratlantisResidentCanBuy(GameTestHelper helper) {
        PlayerStallBlockEntity stall = stall(helper);
        ResourceLocation id = new ResourceLocation("rats", "rat");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "Rats must be available in this pack");
        var entity = BuiltInRegistries.ENTITY_TYPE.get(id).create(helper.getLevel());
        helper.assertTrue(entity instanceof PathfinderMob, "Rat must be a pathfinding resident");
        PathfinderMob rat = (PathfinderMob) entity;
        rat.moveTo(helper.absolutePos(new BlockPos(4, 2, 2)), 0, 0);
        rat.getPersistentData().putBoolean("better_spirit_commerce:resident_rat", true);
        helper.getLevel().addFreshEntity(rat);
        ResidentState state = ResidentState.of(rat);
        state.setNeeds(5, 18, 18);
        state.add(new ItemStack(Items.OAK_LOG, 8));
        stall.seedStock(new ItemStack(Items.CARROT, 4));
        stall.configure(0, new ItemStack(Items.CARROT, 2), new ItemStack(Items.OAK_LOG, 3), true);
        helper.assertTrue(ResidentRules.isResident(rat)
                        && PlayerStallTrade.quote(helper.getLevel(), stall, rat, 0).available()
                        && PlayerStallTrade.execute(helper.getLevel(), stall, rat, 0)
                        && stall.stockCount(new ItemStack(Items.CARROT)) == 2
                        && PlayerStallBlockEntity.count(stall.proceeds(), new ItemStack(Items.OAK_LOG)) == 3,
                "A settlement rat must use the same stocked barter as a villager");
        helper.succeed();
    }

    private static PlayerStallBlockEntity stall(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, PlayerStallRegistries.BLOCK.get());
        PlayerStallBlockEntity stall = (PlayerStallBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        ServerPlayer owner = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.fromString("f992f981-5604-42c9-8993-acd4af41f64d"), "stall_owner"));
        stall.claim(owner);
        return stall;
    }

    private static Villager buyer(GameTestHelper helper) {
        Villager buyer = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 2));
        ResidentState state = ResidentState.of(buyer);
        List<ItemStack> empty = new ArrayList<>();
        for (int i = 0; i < 24; i++) empty.add(ItemStack.EMPTY);
        state.replaceItems(empty);
        state.setNeeds(5, 18, 18);
        return buyer;
    }
}
