package com.bettercontent.economy;

import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.curios.CoinPurseCurio;
import com.bettercontent.economy.curios.CoinPickupRouter;
import com.bettercontent.economy.loot.CoinAcquisition;
import com.bettercontent.economy.loot.EmeraldReplacementLootModifier;
import com.bettercontent.economy.trader.VillagerCatalogue;
import com.bettercontent.economy.trader.WanderingTraderGameTests;
import com.bettercontent.economy.trader.WanderingTraderCatalogue;
import com.bettercontent.economy.trader.WanderingTraderVisits;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.mojang.serialization.Codec;

@Mod(BetterContentEconomy.MOD_ID)
public final class BetterContentEconomy {
    public static final String MOD_ID = "better_content_economy";
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> EMERALD_REPLACEMENT =
            LOOT_MODIFIERS.register("emerald_replacement", () -> EmeraldReplacementLootModifier.CODEC);

    public BetterContentEconomy() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        modBus.addListener(this::registerGameTests);
        LOOT_MODIFIERS.register(modBus);
        MinecraftForge.EVENT_BUS.register(WanderingTraderVisits.class);
        MinecraftForge.EVENT_BUS.register(WanderingTraderCatalogue.class);
        MinecraftForge.EVENT_BUS.register(VillagerCatalogue.class);
        MinecraftForge.EVENT_BUS.register(CoinAcquisition.class);
        MinecraftForge.EVENT_BUS.register(CoinPickupRouter.class);
        CoinPurseCurio.registerPredicate();
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WanderingTraderGameTests.class);
    }
}
