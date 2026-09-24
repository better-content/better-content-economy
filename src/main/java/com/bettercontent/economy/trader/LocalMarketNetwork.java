package com.bettercontent.economy.trader;

import com.bettercontent.economy.BetterContentEconomy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class LocalMarketNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(BetterContentEconomy.MOD_ID, "local_market"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int id;

    private LocalMarketNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Request.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode).consumerMainThread(Request::handle).add();
        CHANNEL.messageBuilder(Select.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Select::encode).decoder(Select::decode).consumerMainThread(Select::handle).add();
        CHANNEL.messageBuilder(Snapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread(Snapshot::handle).add();
    }

    public static void request() { CHANNEL.sendToServer(new Request()); }
    public static void select(LocalMarket.Row row) { CHANNEL.sendToServer(new Select(row.merchant(), row.offerIndex(), row.identity())); }
    private static void send(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(LocalMarket.snapshot(player)));
    }

    public record Request() {
        static void encode(Request packet, FriendlyByteBuf buffer) {}
        static Request decode(FriendlyByteBuf buffer) { return new Request(); }
        static void handle(Request packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.setPacketHandled(true);
            ServerPlayer player = context.getSender();
            if (player != null) context.enqueueWork(() -> send(player));
        }
    }

    public record Select(UUID merchant, int offerIndex, String identity) {
        static void encode(Select packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.merchant()); buffer.writeVarInt(packet.offerIndex()); buffer.writeUtf(packet.identity(), 512);
        }
        static Select decode(FriendlyByteBuf buffer) {
            return new Select(buffer.readUUID(), buffer.readVarInt(), buffer.readUtf(512));
        }
        static void handle(Select packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.setPacketHandled(true);
            ServerPlayer player = context.getSender();
            if (player != null) context.enqueueWork(() -> LocalMarket.openSelected(player,
                    packet.merchant(), packet.offerIndex(), packet.identity()));
        }
    }

    public record Snapshot(List<LocalMarket.Row> rows) {
        public Snapshot { rows = List.copyOf(rows); if (rows.size() > 256) throw new IllegalArgumentException("too many market offers"); }
        static void encode(Snapshot packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.rows().size());
            for (LocalMarket.Row row : packet.rows()) {
                buffer.writeUUID(row.merchant()); buffer.writeVarInt(row.offerIndex());
                buffer.writeUtf(row.merchantName(), 128); buffer.writeInt(row.x()); buffer.writeInt(row.y()); buffer.writeInt(row.z());
                buffer.writeUtf(row.payment(), 256);
                buffer.writeUtf(row.result(), 256); buffer.writeUtf(row.identity(), 512); buffer.writeVarInt(row.usesRemaining());
            }
        }
        static Snapshot decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > 256) throw new IllegalArgumentException("invalid market snapshot size");
            List<LocalMarket.Row> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) rows.add(new LocalMarket.Row(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readUtf(128), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readUtf(256), buffer.readUtf(256), buffer.readUtf(512), buffer.readVarInt()));
            return new Snapshot(rows);
        }
        static void handle(Snapshot packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> LocalMarketClient.receive(packet));
            context.setPacketHandled(true);
        }
    }
}
