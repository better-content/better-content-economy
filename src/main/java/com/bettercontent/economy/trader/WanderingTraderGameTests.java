package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import com.bettercontent.economy.curios.CoinPurseCurio;
import net.minecraft.resources.ResourceLocation;

@PrefixGameTestTemplate(false)
public final class WanderingTraderGameTests {
    private WanderingTraderGameTests() {
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void themedIdentityIsStoredOnTheTrader(final GameTestHelper helper) {
        final WanderingTrader trader = helper.spawn(
                EntityType.WANDERING_TRADER,
                new BlockPos(2, 2, 2));

        WanderingTraderVisits.applyTheme(trader, WanderingTraderTheme.QUARTERMASTER, true);

        if (!"quartermaster".equals(trader.getPersistentData().getString(WanderingTraderVisits.THEME_TAG))) {
            helper.fail("Expected the wandering-trader theme to persist in entity data");
            return;
        }
        if (!WanderingTraderTheme.QUARTERMASTER.displayName().equals(trader.getCustomName())) {
            helper.fail("Expected the wandering trader to use its localized themed name");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void coinRecipesAreAbsent(final GameTestHelper helper) {
        for (Recipe<?> recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
            var id = ForgeRegistries.ITEMS.getKey(result.getItem());
            if (id != null && "createdeco".equals(id.getNamespace())
                    && (id.getPath().endsWith("_coin") || id.getPath().endsWith("_coinstack"))) {
                helper.fail("Coin-producing recipe remained loaded: " + recipe.getId() + " -> " + id);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void themedTraderAlwaysHasOneVillageStarterOffer(final GameTestHelper helper) {
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 2, 2));
        WanderingTraderVisits.applyTheme(trader, WanderingTraderTheme.NATURALIST, false);

        MerchantOffers offers = trader.getOffers();
        long matches = offers.stream().filter(VillageStarterOffer::matches).count();
        long repeatedMatches = trader.getOffers().stream().filter(VillageStarterOffer::matches).count();
        if (matches != 1 || repeatedMatches != 1) {
            helper.fail("Expected exactly one stable village-starter offer");
            return;
        }
        MerchantOffer offer = offers.stream().filter(VillageStarterOffer::matches).findFirst().orElseThrow();
        if (offer.getXp() != 0 || offer.getPriceMultiplier() != 0.0F || offer.getMaxUses() != 1) {
            helper.fail("Village-starter offer metadata did not match the one-use, zero-XP policy");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void merchantCurrencyNormalizationPreservesOfferMetadata(final GameTestHelper helper) {
        MerchantOffer source = new MerchantOffer(
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(Items.EMERALD, 4),
                new ItemStack(Items.EMERALD, 5),
                2,
                9,
                7,
                0.25F,
                3);
        source.addToSpecialPriceDiff(2);
        MerchantOffers offers = new MerchantOffers();
        offers.add(source);

        MerchantCurrencyPolicy.normalize(offers);
        MerchantOffer normalized = offers.get(0);
        ResourceLocation copper = MerchantCurrencyPolicy.COPPER_COIN;
        if (!copper.equals(ForgeRegistries.ITEMS.getKey(normalized.getBaseCostA().getItem()))
                || !copper.equals(ForgeRegistries.ITEMS.getKey(normalized.getCostB().getItem()))
                || !copper.equals(ForgeRegistries.ITEMS.getKey(normalized.getResult().getItem()))) {
            helper.fail("Expected emeralds in every offer position to become copper coins");
            return;
        }
        if (normalized.getBaseCostA().getCount() != 3 || normalized.getCostB().getCount() != 4
                || normalized.getResult().getCount() != 5 || normalized.getUses() != 2
                || normalized.getMaxUses() != 9 || normalized.getXp() != 7
                || normalized.getPriceMultiplier() != 0.25F || normalized.getDemand() != 3
                || normalized.getSpecialPriceDiff() != 2) {
            helper.fail("Merchant metadata changed during currency normalization");
            return;
        }
        MerchantCurrencyPolicy.normalize(offers);
        if (!offers.get(0).createTag().equals(normalized.createTag())) {
            helper.fail("Merchant currency normalization was not idempotent");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void pickedUpCoinsFillThePurseBeforeInventory(final GameTestHelper helper) {
        Item copper = ForgeRegistries.ITEMS.getValue(MerchantCurrencyPolicy.COPPER_COIN);
        if (copper == null) {
            helper.fail("Create Deco copper coin was not registered");
            return;
        }
        ItemStackHandler purse = new ItemStackHandler(CoinPurseCurio.SLOT_COUNT);
        ItemStack remainder = CoinPurseCurio.insertInto(purse, new ItemStack(copper, 10));
        if (!remainder.isEmpty() || purse.getStackInSlot(0).getCount() != 10) {
            helper.fail("Expected the purse insertion boundary to accept all ten coins before inventory fallback");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BetterContentEconomy.MOD_ID, template = "empty", timeoutTicks = 100)
    public static void authoredTradeSignalRequiresCoinPayment(final GameTestHelper helper) {
        WanderingTrader trader = helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 2, 2));
        Item copper = ForgeRegistries.ITEMS.getValue(MerchantCurrencyPolicy.COPPER_COIN);
        if (copper == null) {
            helper.fail("Create Deco copper coin was not registered");
            return;
        }
        MerchantOffer coinPayment = new MerchantOffer(new ItemStack(copper), new ItemStack(Items.BREAD), 1, 0, 0.0F);
        MerchantOffer coinResult = new MerchantOffer(new ItemStack(Items.BREAD), new ItemStack(copper), 1, 0, 0.0F);
        if (!AuthoredTradeSignals.isAuthoredCoinTrade(trader, coinPayment)) {
            helper.fail("Expected a vanilla wandering-trader coin payment to be authored trade evidence");
            return;
        }
        if (AuthoredTradeSignals.isAuthoredCoinTrade(trader, coinResult)) {
            helper.fail("A coin result must not be reported as coin spending");
            return;
        }
        helper.succeed();
    }
}
