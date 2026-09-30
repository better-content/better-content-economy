package com.bettercontent.spiritcommerce.resident;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Small command channel; every mutation checks ownership, distance, and current stock. */
public final class PlayerStallNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterSpiritCommerce.MOD_ID, "player_stall"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private PlayerStallNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread(Snapshot::handle).add();
        CHANNEL.messageBuilder(Request.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode).consumerMainThread(Request::handle).add();
    }

    public static void open(ServerPlayer player, PlayerStallBlockEntity stall) { send(player, stall, true, ""); }

    public static void send(ServerPlayer player, PlayerStallBlockEntity stall, boolean open, String message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(stall.getBlockPos(),
                stall.owns(player), stall.visiting(), stall.stock(), stall.proceeds(), stall.offers(),
                statuses(player, stall), stall.events(), open, message));
    }

    private static List<String> statuses(ServerPlayer player, PlayerStallBlockEntity stall) {
        List<LivingEntity> buyers = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(stall.getBlockPos()).inflate(24, 8, 24),
                entity -> entity.isAlive() && ResidentRules.isResident(entity));
        if (buyers.size() > 8) buyers = buyers.subList(0, 8);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < PlayerStallBlockEntity.OFFERS; i++) {
            PlayerStallBlockEntity.Offer offer = stall.offers().get(i);
            if (!offer.enabled() || !offer.valid()) { result.add("Offer disabled"); continue; }
            if (stall.stockCount(offer.sale()) < offer.sale().getCount()) { result.add("Out of stock"); continue; }
            String status = buyers.isEmpty() ? "No residents nearby" : "No current demand";
            for (LivingEntity buyer : buyers) {
                PlayerStallTrade.Quote quote = PlayerStallTrade.quote(player.serverLevel(), stall, buyer, i);
                if (quote.available()) {
                    String name = buyer.getDisplayName().getString();
                    status = "Can buy: " + name.substring(0, Math.min(name.length(), 100));
                    break;
                }
                if (!quote.blocker().equals("No current demand")) status = quote.blocker();
            }
            result.add(status);
        }
        return result;
    }

    public static void request(BlockPos pos, int operation, int index, ItemStack sale,
                               ItemStack payment, boolean enabled) {
        CHANNEL.sendToServer(new Request(pos, operation, index, sale, payment, enabled));
    }

    public record Snapshot(BlockPos pos, boolean owner, boolean visiting, List<ItemStack> stock,
                           List<ItemStack> proceeds, List<PlayerStallBlockEntity.Offer> offers,
                           List<String> statuses, List<String> events, boolean open, String message) {
        static void encode(Snapshot p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos()); b.writeBoolean(p.owner()); b.writeBoolean(p.visiting());
            for (ItemStack stack : p.stock()) b.writeItem(stack);
            for (ItemStack stack : p.proceeds()) b.writeItem(stack);
            for (PlayerStallBlockEntity.Offer offer : p.offers()) {
                b.writeItem(offer.sale()); b.writeItem(offer.payment()); b.writeBoolean(offer.enabled());
            }
            for (String status : p.statuses()) b.writeUtf(status, 128);
            b.writeVarInt(p.events().size());
            for (String line : p.events()) b.writeUtf(line, 256);
            b.writeBoolean(p.open()); b.writeUtf(p.message(), 128);
        }
        static Snapshot decode(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos(); boolean owner = b.readBoolean(); boolean visiting = b.readBoolean();
            List<ItemStack> stock = new ArrayList<>(), proceeds = new ArrayList<>();
            for (int i = 0; i < PlayerStallBlockEntity.SLOTS; i++) stock.add(b.readItem());
            for (int i = 0; i < PlayerStallBlockEntity.SLOTS; i++) proceeds.add(b.readItem());
            List<PlayerStallBlockEntity.Offer> offers = new ArrayList<>();
            for (int i = 0; i < PlayerStallBlockEntity.OFFERS; i++)
                offers.add(new PlayerStallBlockEntity.Offer(b.readItem(), b.readItem(), b.readBoolean()));
            List<String> statuses = new ArrayList<>();
            for (int i = 0; i < PlayerStallBlockEntity.OFFERS; i++) statuses.add(b.readUtf(128));
            int size = b.readVarInt();
            if (size < 0 || size > 16) throw new IllegalArgumentException("Invalid stall history");
            List<String> events = new ArrayList<>();
            for (int i = 0; i < size; i++) events.add(b.readUtf(256));
            return new Snapshot(pos, owner, visiting, stock, proceeds, offers, statuses, events,
                    b.readBoolean(), b.readUtf(128));
        }
        static void handle(Snapshot p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); context.setPacketHandled(true);
            context.enqueueWork(() -> PlayerStallClient.receive(p));
        }
    }

    public record Request(BlockPos pos, int operation, int index, ItemStack sale,
                          ItemStack payment, boolean enabled) {
        static void encode(Request p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos()); b.writeVarInt(p.operation()); b.writeVarInt(p.index());
            b.writeItem(p.sale()); b.writeItem(p.payment()); b.writeBoolean(p.enabled());
        }
        static Request decode(FriendlyByteBuf b) {
            return new Request(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readItem(),
                    b.readItem(), b.readBoolean());
        }
        static void handle(Request p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); context.setPacketHandled(true);
            ServerPlayer player = context.getSender();
            if (player != null) context.enqueueWork(() -> process(player, p));
        }
    }

    private static void process(ServerPlayer player, Request request) {
        if (player.blockPosition().distSqr(request.pos()) > 64
                || !(player.serverLevel().getBlockEntity(request.pos()) instanceof PlayerStallBlockEntity stall)) return;
        if (request.operation() == 0) { send(player, stall, false, ""); return; }
        if (!stall.owns(player)) { send(player, stall, false, "Only the owner can change this stall"); return; }
        boolean success = switch (request.operation()) {
            case 1 -> stall.deposit(player, request.index());
            case 2 -> stall.withdraw(player, false, request.index());
            case 3 -> stall.withdraw(player, true, request.index());
            case 4 -> configure(stall, request);
            default -> false;
        };
        send(player, stall, false, success ? "Saved" : "Could not complete action");
    }

    private static boolean configure(PlayerStallBlockEntity stall, Request p) {
        if (p.index() < 0 || p.index() >= PlayerStallBlockEntity.OFFERS) return false;
        if (p.sale().isEmpty() && p.payment().isEmpty())
            return stall.configure(p.index(), ItemStack.EMPTY, ItemStack.EMPTY, false);
        return !p.sale().isEmpty() && !p.payment().isEmpty()
                && stall.configure(p.index(), p.sale(), p.payment(), p.enabled());
    }
}
