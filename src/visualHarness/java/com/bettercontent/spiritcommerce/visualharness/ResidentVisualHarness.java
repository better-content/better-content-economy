package com.bettercontent.spiritcommerce.visualharness;

import com.bettercontent.spiritcommerce.resident.ResidentRules;
import com.bettercontent.spiritcommerce.resident.ResidentState;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Non-shipping production-screen fixtures and screenshot command. */
final class ResidentVisualHarness {
    private static final String[] SCENES = {"populated", "blocked", "empty", "narrow", "long_name"};
    private static final Map<UUID, Integer> AUTO_STAGE = new java.util.HashMap<>();
    private static final Map<UUID, Long> AUTO_NEXT = new java.util.HashMap<>();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(CampVisualHarness.MOD_ID, "resident_visual"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
    private ResidentVisualHarness() {}

    static void register() {
        CHANNEL.messageBuilder(Show.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Show::encode).decoder(Show::decode).consumerMainThread(Show::handle).add();
        CHANNEL.messageBuilder(Capture.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Capture::encode).decoder(Capture::decode).consumerMainThread(Capture::handle).add();
    }

    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("residentvisual").requires(source -> source.hasPermission(2))
                .then(Commands.literal("prepare").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> prepare(EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("fixture").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> fixture(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("scene", StringArgumentType.word())
                                .executes(context -> show(EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "scene"))))))
                .then(Commands.literal("capture").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> capture(EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "name"), false)))))
                .then(Commands.literal("captureworld").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> capture(EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "name"), true))))));
    }

    static void autoCapture(ServerPlayer player) {
        prepare(player);
        AUTO_STAGE.put(player.getUUID(), 0);
        AUTO_NEXT.put(player.getUUID(), player.serverLevel().getGameTime() + 60);
    }

    static void autoTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Integer stage = AUTO_STAGE.get(player.getUUID());
            if (stage == null || player.serverLevel().getGameTime() < AUTO_NEXT.get(player.getUUID())) continue;
            if (stage >= SCENES.length) { AUTO_STAGE.remove(player.getUUID()); AUTO_NEXT.remove(player.getUUID()); continue; }
            String scene = SCENES[stage];
            show(player, scene);
            capture(player, "resident-" + scene, false);
            AUTO_STAGE.put(player.getUUID(), stage + 1);
            AUTO_NEXT.put(player.getUUID(), player.serverLevel().getGameTime() + 120);
        }
    }

    private static int prepare(ServerPlayer player) {
        player.getInventory().items.set(0, new ItemStack(Items.CARROT, 16));
        player.getInventory().items.set(1, new ItemStack(Items.OAK_LOG, 12));
        player.getInventory().items.set(2, new ItemStack(Items.COAL, 8));
        player.getInventory().items.set(3, new ItemStack(Items.GLASS_BOTTLE, 5));
        player.getInventory().setChanged();
        return 1;
    }

    private static int fixture(net.minecraft.commands.CommandSourceStack source, ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos center = player.blockPosition().offset(4, 0, 0);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -3), center.offset(4, -1, 3)))
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -3), center.offset(4, 2, 3)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(center.offset(2, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        level.setBlockAndUpdate(center.offset(2, 0, 1), Blocks.FURNACE.defaultBlockState());
        level.setBlockAndUpdate(center.offset(2, 0, -1), Blocks.STONECUTTER.defaultBlockState());
        level.setBlockAndUpdate(center.offset(3, 0, 0), Blocks.WHITE_BED.defaultBlockState());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 3, -2), center.offset(3, 3, 2)))
            level.setBlockAndUpdate(pos, Blocks.OAK_SLAB.defaultBlockState());
        Villager grower = EntityType.VILLAGER.create(level);
        Villager maker = EntityType.VILLAGER.create(level);
        if (grower == null || maker == null) return 0;
        grower.moveTo(center.getX() + .5, center.getY(), center.getZ() + .5, 0, 0);
        maker.moveTo(center.getX() + 1.5, center.getY(), center.getZ() - .5, 0, 0);
        level.addFreshEntity(grower); level.addFreshEntity(maker);
        grower.setNoAi(true); maker.setNoAi(true);
        ResidentRules.setWorksite(grower, center);
        ResidentRules.setWorksite(maker, center);
        grower.setCustomName(net.minecraft.network.chat.Component.literal("Fixture Grower"));
        maker.setCustomName(net.minecraft.network.chat.Component.literal("Fixture Maker"));
        ResidentState first = ResidentState.of(grower);
        first.add(new ItemStack(Items.CARROT, 12));
        first.add(new ItemStack(Items.OAK_LOG, 8));
        first.setNeeds(17, 5, 16);
        ResidentState second = ResidentState.of(maker);
        second.add(ResidentRules.waterBottle(3).copyWithCount(6));
        second.add(new ItemStack(Items.COBBLESTONE, 12));
        second.setNeeds(5, 17, 16);
        prepare(player);
        JsonObject json = new JsonObject();
        json.addProperty("version", "bc.villagers.v1");
        json.addProperty("grower", grower.getUUID().toString());
        json.addProperty("maker", maker.getUUID().toString());
        json.addProperty("dimension", level.dimension().location().toString());
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("BCV1 " + json), false);
        return 1;
    }

    private static int show(ServerPlayer player, String scene) {
        if (!List.of("populated", "blocked", "empty", "narrow", "long_name", "world").contains(scene)) return 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Show(scene));
        return 1;
    }

    private static int capture(ServerPlayer player, String name, boolean world) {
        if (!name.matches("resident-[a-z0-9_-]{1,40}")) return 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Capture(name, world));
        return 1;
    }

    private record Show(String scene) {
        static void encode(Show packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.scene(), 32); }
        static Show decode(FriendlyByteBuf buffer) { return new Show(buffer.readUtf(32)); }
        static void handle(Show packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
            var context = supplier.get(); context.setPacketHandled(true);
            context.enqueueWork(() -> ResidentVisualClient.show(packet.scene()));
        }
    }

    private record Capture(String name, boolean world) {
        static void encode(Capture packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.name(), 64); buffer.writeBoolean(packet.world());
        }
        static Capture decode(FriendlyByteBuf buffer) { return new Capture(buffer.readUtf(64), buffer.readBoolean()); }
        static void handle(Capture packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
            var context = supplier.get(); context.setPacketHandled(true);
            context.enqueueWork(() -> ResidentVisualClient.capture(packet.name(), packet.world()));
        }
    }
}
