package com.bettercontent.economy.visualharness;

import com.bettercontent.economy.resident.ResidentBarter;
import com.bettercontent.economy.resident.ResidentNetwork;
import com.bettercontent.economy.resident.ResidentScreen;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class ResidentVisualClient {
    private static String pending;
    private static boolean pendingWorld;
    private static int settleTicks;
    private ResidentVisualClient() {}

    static void show(String scene) {
        Minecraft client = Minecraft.getInstance();
        if (scene.equals("world")) {
            client.setScreen(null);
            return;
        }
        boolean narrow = scene.equals("narrow");
        client.getWindow().setWindowed(narrow ? 960 : 1600, narrow ? 720 : 900);
        client.options.guiScale().set(narrow ? 3 : 2);
        client.resizeDisplay();
        ItemStack bread = new ItemStack(Items.BREAD, 3);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 8);
        ItemStack tool = new ItemStack(Items.STONE_PICKAXE);
        ItemStack longName = new ItemStack(Items.BOOK);
        longName.setHoverName(Component.literal("Polished Bookcase of the Eastern Workshop"));
        List<ResidentBarter.Offer> offers = scene.equals("empty") ? List.of() : List.of(
                new ResidentBarter.Offer(bread, null, "In stock"),
                new ResidentBarter.Offer(planks, new ResourceLocation("minecraft", "oak_planks"), "Can make"),
                new ResidentBarter.Offer(tool, null, "In stock"),
                new ResidentBarter.Offer(longName, null, "In stock"));
        ResidentNetwork.Snapshot snapshot = new ResidentNetwork.Snapshot(UUID.randomUUID(),
                scene.equals("long_name") ? "Mara the exceptionally patient village carpenter" : "Mara the Carpenter",
                scene.equals("blocked") ? "Needs safe water" : "Food and water supplied",
                scene.equals("blocked") ? "Waiting for fuel to purify water" : "Preparing timber for trade",
                offers, true, "");
        ResidentScreen screen = new ResidentScreen(snapshot);
        client.setScreen(screen);
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().getWindow(), 2, 2);
        if (scene.equals("blocked")) screen.quote(new ResidentNetwork.QuoteResult(false,
                "Needs a heated cooking pot and two oak logs before this trade can proceed"));
    }

    static void capture(String name, boolean world) {
        pending = name + ".png";
        pendingWorld = world;
        settleTicks = 0;
        MinecraftForge.EVENT_BUS.register(ResidentVisualClient.class);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending == null
                || !pendingWorld && !(Minecraft.getInstance().screen instanceof ResidentScreen)
                || ++settleTicks < 30) return;
        Minecraft client = Minecraft.getInstance();
        String file = pending;
        pending = null;
        Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(),
                message -> System.out.println("RESIDENT_VISUAL " + file + " " + message.getString()));
        MinecraftForge.EVENT_BUS.unregister(ResidentVisualClient.class);
    }
}
