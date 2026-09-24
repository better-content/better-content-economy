package com.bettercontent.economy;

import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.registry.SpiritProfessions;
import com.bettercontent.economy.registry.CurrencyItems;
import com.bettercontent.economy.spirit.SpiritAcquisition;
import com.bettercontent.economy.spirit.SpiritPouchAccess;
import com.bettercontent.economy.trader.ProfessionBehaviors;
import com.bettercontent.economy.trader.AuthoredTradeSignals;
import com.bettercontent.economy.trader.VillagerCatalogue;
import com.bettercontent.economy.trader.WanderingTraderGameTests;
import com.bettercontent.economy.trader.WanderingTraderCatalogue;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import com.bettercontent.economy.trader.LocalMarketNetwork;
import com.bettercontent.economy.trader.TraderCampRegistries;
import com.bettercontent.economy.ops.ObservationalExportCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BetterContentEconomy.MOD_ID)
public final class BetterContentEconomy {
    public static final String MOD_ID = "better_content_economy";
    public BetterContentEconomy() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        modBus.addListener(this::registerGameTests);
        LocalMarketNetwork.register();
        TraderCampRegistries.register(modBus);
        SpiritProfessions.register(modBus);
        CurrencyItems.register(modBus);
        MinecraftForge.EVENT_BUS.register(WanderingTraderVisits.class);
        MinecraftForge.EVENT_BUS.register(WanderingTraderCatalogue.class);
        MinecraftForge.EVENT_BUS.register(VillagerCatalogue.class);
        MinecraftForge.EVENT_BUS.register(SpiritAcquisition.class);
        MinecraftForge.EVENT_BUS.register(ProfessionBehaviors.class);
        MinecraftForge.EVENT_BUS.register(AuthoredTradeSignals.class);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            SpiritPouchAccess.register(event.getDispatcher());
            ObservationalExportCommand.register(event.getDispatcher());
        });
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WanderingTraderGameTests.class);
    }
}
