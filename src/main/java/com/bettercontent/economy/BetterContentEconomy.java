package com.bettercontent.economy;

import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.curios.CoinPurseCurio;
import com.bettercontent.economy.trader.WanderingTraderGameTests;
import com.bettercontent.economy.trader.WanderingTraderCatalogue;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BetterContentEconomy.MOD_ID)
public final class BetterContentEconomy {
    public static final String MOD_ID = "better_content_economy";

    public BetterContentEconomy() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerGameTests);
        MinecraftForge.EVENT_BUS.register(WanderingTraderVisits.class);
        MinecraftForge.EVENT_BUS.register(WanderingTraderCatalogue.class);
        CoinPurseCurio.registerPredicate();
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WanderingTraderGameTests.class);
    }
}
