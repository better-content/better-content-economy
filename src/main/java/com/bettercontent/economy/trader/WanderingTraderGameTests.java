package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.mixin.FloatingEntityAccessor;
import com.bettercontent.economy.registry.SpiritProfessions;
import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;

@PrefixGameTestTemplate(false)
public final class WanderingTraderGameTests {
    private WanderingTraderGameTests() {}

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void themedIdentityIsStored(final GameTestHelper helper) {
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 2, 2));
        WanderingTraderVisits.applyTheme(trader, WanderingTraderTheme.INFERNAL, true);
        if (!"infernal".equals(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG))) {
            helper.fail("Expected the spirit theme to persist");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void everyThemedTraderHasThirteenMatchingGoodsAndEggs(final GameTestHelper helper) {
        for (WanderingTraderTheme theme : WanderingTraderTheme.values()) {
            WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 2, 2));
            WanderingTraderVisits.applyTheme(trader, theme, false);
            MerchantOffers offers = trader.getOffers();
            long eggs = offers.stream().map(MerchantOffer::getResult)
                    .filter(stack -> stack.is(Items.VILLAGER_SPAWN_EGG)).count();
            boolean matchingPayment = offers.stream().allMatch(offer ->
                    offer.getBaseCostA().is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(theme.spirit().itemId())));
            MerchantOffer eggOffer = offers.stream().filter(offer -> offer.getResult().is(Items.VILLAGER_SPAWN_EGG))
                    .findFirst().orElse(null);
            String profession = eggOffer == null ? "" : eggOffer.getResult().getOrCreateTagElement("EntityTag")
                    .getCompound("VillagerData").getString("profession");
            String expectedProfession = SpiritProfessions.definition(theme.spirit()).profession().getId().toString();
            if (offers.size() != 14 || eggs != 1 || !matchingPayment || !expectedProfession.equals(profession)) {
                helper.fail("Invalid " + theme.id() + " market: offers=" + offers.size()
                        + " eggs=" + eggs + " payment=" + matchingPayment + " profession=" + profession);
                return;
            }
            trader.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void spiritVillagersHaveReadableNames(final GameTestHelper helper) {
        for (WanderingTraderTheme theme : WanderingTraderTheme.values()) {
            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
            villager.setVillagerData(villager.getVillagerData()
                    .setProfession(SpiritProfessions.definition(theme.spirit()).profession().get()));
            String expectedName = SpiritProfessions.definition(theme.spirit()).fallbackName();
            if (!expectedName.equals(villager.getName().getString())) {
                helper.fail("Unreadable " + theme.id() + " villager name: " + villager.getName().getString());
                return;
            }
            villager.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void creditedKillUsesMalumAnimatedSpiritEntity(final GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.nameUUIDFromBytes("spirit-economy-gametest".getBytes()), "spirit-economy-test"));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
        AABB bounds = zombie.getBoundingBox().inflate(6.0D);
        helper.succeedWhen(() -> {
            var spirits = helper.getLevel().getEntitiesOfClass(SpiritItemEntity.class, bounds);
            helper.assertTrue(!spirits.isEmpty(), "Credited hostile kill did not release a Malum SpiritItemEntity");
            helper.assertTrue(spirits.stream().allMatch(spirit ->
                            ((FloatingEntityAccessor) spirit).betterContentEconomy$getOwnerUuid().equals(player.getUUID())),
                    "Released spirit did not target the credited player");
        });
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void emeraldOffersAreRemoved(final GameTestHelper helper) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new net.minecraft.world.item.ItemStack(Items.EMERALD),
                new net.minecraft.world.item.ItemStack(Items.BREAD), 1, 0, 0.0F));
        MerchantCurrencyPolicy.normalize(offers);
        if (!offers.isEmpty()) {
            helper.fail("Emerald offer survived spirit-only normalization");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void plagueDoctorUsesDailySpiritOddities(final GameTestHelper helper) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(PlagueDoctorCatalogue.PLAGUE_DOCTOR);
        if (type == null) {
            helper.fail("Rats Plague Doctor was not available in the GameTest runtime");
            return;
        }
        Entity created = type.create(helper.getLevel());
        if (!(created instanceof AbstractVillager doctor)) {
            helper.fail("Rats Plague Doctor was not an AbstractVillager");
            return;
        }
        doctor.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(doctor);
        MerchantOffers offers = doctor.getOffers();
        boolean spiritPayments = offers.stream().allMatch(offer -> {
            var id = ForgeRegistries.ITEMS.getKey(offer.getBaseCostA().getItem());
            return id != null && "malum".equals(id.getNamespace()) && id.getPath().endsWith("_spirit");
        });
        long uniqueResults = offers.stream().map(offer -> ForgeRegistries.ITEMS.getKey(offer.getResult().getItem()))
                .distinct().count();
        if (offers.size() != 8 || uniqueResults != 8 || !spiritPayments || !AuthoredTradeSignals.isAuthored(doctor)) {
            helper.fail("Invalid plague doctor catalogue: offers=" + offers.size()
                    + " unique=" + uniqueResults + " spiritPayments=" + spiritPayments);
            return;
        }

        MerchantOffer exhausted = offers.get(0);
        while (!exhausted.isOutOfStock()) exhausted.increaseUses();
        long currentDay = doctor.getPersistentData().getLong(PlagueDoctorCatalogue.DAY_TAG);
        helper.runAfterDelay(5, () -> {
            if (!exhausted.isOutOfStock()) {
                helper.fail("Rats native restock refreshed an authored offer during the same day");
                return;
            }
            PlagueDoctorCatalogue.ensureOffers(doctor, offers, currentDay + 1L);
            if (offers.size() != 8 || offers.stream().anyMatch(MerchantOffer::isOutOfStock)
                    || doctor.getPersistentData().getLong(PlagueDoctorCatalogue.DAY_TAG) != currentDay + 1L) {
                helper.fail("Plague Doctor did not receive fresh stock for the next day");
                return;
            }
            doctor.discard();
            helper.succeed();
        });
    }
}
