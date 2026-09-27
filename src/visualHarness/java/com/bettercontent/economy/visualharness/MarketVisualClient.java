package com.bettercontent.economy.visualharness;

import com.bettercontent.economy.trader.LocalMarketNetwork;
import com.bettercontent.economy.trader.LocalMarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;

import java.util.List;

/** Client-only half of the non-shipping visual fixture. */
final class MarketVisualClient {
    private MarketVisualClient() {}

    static void show(String scene) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        minecraft.getWindow().setWindowed(scene.equals("narrow") ? 960 : 1600,
                scene.equals("narrow") ? 720 : 900);
        minecraft.options.guiScale().set(scene.equals("narrow") ? 3 : 2);
        minecraft.resizeDisplay();
        LocalMarketScreen screen = new LocalMarketScreen(null);
        minecraft.setScreen(screen);
        if (scene.equals("empty")) {
            screen.update(new LocalMarketNetwork.Snapshot(List.of(), false));
        } else {
            if (scene.equals("filtered")) screen.setSearchText("lantern");
            if (scene.equals("no_matches")) screen.setSearchText("unobtainium");
            LocalMarketNetwork.request();
        }
    }
}
