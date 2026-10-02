package com.bettercontent.spiritcommerce.resident;

import net.minecraft.client.Minecraft;

public final class ResidentClient {
    private ResidentClient() {}

    static void receive(ResidentNetwork.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (snapshot.open()) client.setScreen(new ResidentScreen(snapshot));
        else if (client.screen instanceof ResidentScreen screen) screen.update(snapshot);
    }

    static void quote(ResidentNetwork.QuoteResult result) {
        if (Minecraft.getInstance().screen instanceof ResidentScreen screen) screen.quote(result);
    }
}
