package com.bettercontent.economy.trader;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Console-controlled real client screenshots of the production post renderer. */
@Mod(CampVisualHarness.MOD_ID)
public final class CampVisualHarness {
    public static final String MOD_ID = "better_content_economy_visual_harness";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MOD_ID, "capture"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
    private static BlockPos origin;
    private static int floorY;

    public CampVisualHarness() {
        CHANNEL.messageBuilder(CapturePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CapturePacket::encode).decoder(CapturePacket::decode)
                .consumerMainThread(CapturePacket::handle).add();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("campvisual").requires(source -> source.hasPermission(2))
                .then(Commands.literal("prepare").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> prepare(EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("view").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("angle", StringArgumentType.word()).executes(context -> view(
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "angle"))))))
                .then(Commands.literal("capture").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.word()).executes(context -> capture(
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "name")))))));
    }

    private static int prepare(final ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            player.sendSystemMessage(Component.literal("Run campvisual from the Overworld."));
            return 0;
        }
        floorY = Math.max(level.getMinBuildHeight() + 8, player.blockPosition().getY() - 1);
        origin = new BlockPos(player.blockPosition().getX() - 8, floorY + 1,
                player.blockPosition().getZ() - 18);
        for (int x = -2; x <= 19; x++) {
            for (int z = -2; z <= 23; z++) {
                BlockPos floor = origin.offset(x, -1, z);
                level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int y = 0; y < 7; y++) level.setBlock(floor.above(y + 1), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        WanderingTraderTheme[] themes = WanderingTraderTheme.values();
        for (int index = 0; index < themes.length; index++) {
            int column = index % 4;
            int row = index / 4;
            double x = origin.getX() + 2.0D + column * 4.0D;
            double z = origin.getZ() + 4.0D + row * 8.0D;
            WanderingTrader trader = EntityType.WANDERING_TRADER.create(level);
            if (trader == null) continue;
            trader.moveTo(x, origin.getY(), z, 180.0F, 0.0F);
            level.addFreshEntity(trader);
            WanderingTraderVisits.applyTheme(trader, themes[index], true);
            if (TraderCampService.createForVisit(trader, themes[index])) {
                BlockPos target = BlockPos.of(trader.getPersistentData()
                        .getLong(TraderCampService.CAMP_POS_TAG)).relative(Direction.NORTH);
                trader.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 180.0F, 0.0F);
            }
            trader.setNoAi(true);
        }
        player.setNoGravity(true);
        view(player, "overview");
        player.sendSystemMessage(Component.literal("Camp scene ready. Capture overview, profile, and detail views with /campvisual capture."));
        return 1;
    }

    private static int view(final ServerPlayer player, final String angle) {
        if (origin == null) {
            player.sendSystemMessage(Component.literal("Run /campvisual prepare first."));
            return 0;
        }
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        switch (angle) {
            case "profile" -> {
                x = origin.getX() - 6.5D; y = floorY + 3.8D; z = origin.getZ() + 9.0D;
                yaw = 270.0F; pitch = 8.0F;
            }
            case "detail" -> {
                x = origin.getX() + 2.0D; y = floorY + 3.4D; z = origin.getZ() + 12.0D;
                yaw = 180.0F; pitch = 12.0F;
            }
            case "overview" -> {
                x = origin.getX() + 6.0D; y = floorY + 9.0D; z = origin.getZ() + 25.0D;
                yaw = 180.0F; pitch = 23.0F;
            }
            default -> {
                player.sendSystemMessage(Component.literal("Angles: overview, profile, detail."));
                return 0;
            }
        }
        player.connection.teleport(x, y, z, yaw, pitch);
        player.setNoGravity(true);
        return 1;
    }

    private static int capture(final ServerPlayer player, final String name) {
        List<String> names = List.of("camps-overview", "awning-profile", "awning-detail");
        if (!names.contains(name)) {
            player.sendSystemMessage(Component.literal("Capture names: camps-overview, awning-profile, awning-detail."));
            return 0;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CapturePacket(name));
        player.sendSystemMessage(Component.literal("Camp screenshot requested: " + name));
        return 1;
    }

    private record CapturePacket(String name) {
        private static void encode(final CapturePacket packet, final FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.name(), 64);
        }
        private static CapturePacket decode(final FriendlyByteBuf buffer) {
            return new CapturePacket(buffer.readUtf(64));
        }
        private static void handle(final CapturePacket packet,
                                   final java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
            context.get().setPacketHandled(true);
            CampVisualScreenshot.request(packet.name());
        }
    }
}
