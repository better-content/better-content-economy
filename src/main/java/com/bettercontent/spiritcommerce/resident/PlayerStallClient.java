package com.bettercontent.spiritcommerce.resident;

import net.minecraft.client.Minecraft;

public final class PlayerStallClient {
    private PlayerStallClient() {}
    static void receive(PlayerStallNetwork.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (snapshot.open()) client.setScreen(new PlayerStallScreen(snapshot));
        else if (client.screen instanceof PlayerStallScreen screen) screen.update(snapshot);
    }
}
