package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
}
