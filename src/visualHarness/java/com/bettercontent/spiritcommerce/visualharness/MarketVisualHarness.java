package com.bettercontent.spiritcommerce.visualharness;

import com.bettercontent.spiritcommerce.trader.LocalMarket;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

/** Non-shipping, server-controlled fixtures rendered by the production market screen. */
final class MarketVisualHarness {
    private static final String ID = CampVisualHarness.MOD_ID;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ID, "market_visual"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
    private static final String FIXTURE_TAG = "better_spirit_commerce_market_visual";

    private MarketVisualHarness() {}

    static void register() {
        CHANNEL.messageBuilder(ShowPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShowPacket::encode).decoder(ShowPacket::decode)
                .consumerMainThread(ShowPacket::handle).add();
        CHANNEL.messageBuilder(CapturePacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CapturePacket::encode).decoder(CapturePacket::decode)
                .consumerMainThread(CapturePacket::handle).add();
    }

    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("marketvisual").requires(source -> source.hasPermission(2))
                .then(Commands.literal("prepare").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> prepare(EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("scene", StringArgumentType.word()).executes(context -> show(
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "scene"))))))
                .then(Commands.literal("capture").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.word()).executes(context -> capture(
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("select").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("result", StringArgumentType.word()).executes(context -> select(
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "result")))))));
    }

    private static int prepare(ServerPlayer player) {
        for (Villager old : player.serverLevel().getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(32), villager -> villager.getTags().contains(FIXTURE_TAG)
                        || villager.hasCustomName() && List.of("Cartographer of the Eastern Lantern Roads",
                                "Village Provisioner").contains(villager.getCustomName().getString()))) {
            old.discard();
        }
        Villager first = create(player, 2, 1, "Cartographer of the Eastern Lantern Roads");
        Villager second = create(player, -2, 2, "Village Provisioner");
        if (first == null || second == null) return 0;
        first.getOffers().clear();
        first.getOffers().add(offer(Items.AMETHYST_SHARD, 3, Items.COPPER_INGOT, 2, Items.LANTERN, 1));
        first.getOffers().add(offer(Items.AMETHYST_SHARD, 4, Items.AIR, 0, Items.COMPASS, 1));
        first.getOffers().add(offer(Items.AMETHYST_SHARD, 2, Items.AIR, 0, Items.MAP, 1));
        first.getOffers().add(offer(Items.AMETHYST_SHARD, 5, Items.AIR, 0, Items.SPYGLASS, 1));
        first.getOffers().add(offer(Items.AMETHYST_SHARD, 1, Items.AIR, 0, Items.ITEM_FRAME, 2));
        second.getOffers().clear();
        second.getOffers().add(offer(Items.COPPER_INGOT, 3, Items.AIR, 0, Items.BREAD, 4));
        second.getOffers().add(offer(Items.COPPER_INGOT, 4, Items.AIR, 0, Items.APPLE, 3));
        second.getOffers().add(offer(Items.COPPER_INGOT, 2, Items.AIR, 0, Items.BOOK, 1));
        second.getOffers().add(offer(Items.COPPER_INGOT, 6, Items.AIR, 0, Items.CLOCK, 1));
        second.getOffers().add(offer(Items.COPPER_INGOT, 1, Items.AIR, 0, Items.TORCH, 8));
        player.sendSystemMessage(Component.literal("Market fixture ready: 2 merchants, 10 offers."));
        return 1;
    }

    private static Villager create(ServerPlayer player, int dx, int dz, String name) {
        Villager villager = EntityType.VILLAGER.create(player.serverLevel());
        if (villager == null) return null;
        villager.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 0, 0);
        villager.setCustomName(Component.literal(name));
        villager.setNoAi(true);
        villager.addTag(FIXTURE_TAG);
        player.serverLevel().addFreshEntity(villager);
        return villager;
    }

    private static MerchantOffer offer(net.minecraft.world.item.Item payment, int paymentCount,
                                       net.minecraft.world.item.Item second, int secondCount,
                                       net.minecraft.world.item.Item result, int resultCount) {
        ItemStack costB = secondCount == 0 ? ItemStack.EMPTY : new ItemStack(second, secondCount);
        return new MerchantOffer(new ItemStack(payment, paymentCount), costB,
                new ItemStack(result, resultCount), 12, 1, 0);
    }

    private static int show(ServerPlayer player, String scene) {
        if (!List.of("populated", "filtered", "empty", "no_matches", "narrow").contains(scene)) return 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ShowPacket(scene));
        return 1;
    }

    private static int capture(ServerPlayer player, String name) {
        if (!List.of("market-populated", "market-filtered", "market-empty", "market-no-matches",
                "market-narrow", "market-selected")
                .contains(name)) return 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CapturePacket(name));
        return 1;
    }

    private static int select(ServerPlayer player, String result) {
        return LocalMarket.snapshot(player).stream()
                .filter(row -> row.result().toLowerCase(java.util.Locale.ROOT).contains(
                        result.toLowerCase(java.util.Locale.ROOT)))
                .findFirst().map(row -> {
                    LocalMarket.openSelected(player, row.merchant(), row.offerIndex(), row.identity());
                    return 1;
                }).orElse(0);
    }

    private record ShowPacket(String scene) {
        private static void encode(ShowPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.scene(), 32);
        }
        private static ShowPacket decode(FriendlyByteBuf buffer) {
            return new ShowPacket(buffer.readUtf(32));
        }
        private static void handle(ShowPacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
            supplier.get().setPacketHandled(true);
            MarketVisualClient.show(packet.scene());
        }
    }

    private record CapturePacket(String name) {
        private static void encode(CapturePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.name(), 64);
        }
        private static CapturePacket decode(FriendlyByteBuf buffer) {
            return new CapturePacket(buffer.readUtf(64));
        }
        private static void handle(CapturePacket packet,
                                   Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
            supplier.get().setPacketHandled(true);
            MarketVisualScreenshot.request(packet.name());
        }
    }
}
