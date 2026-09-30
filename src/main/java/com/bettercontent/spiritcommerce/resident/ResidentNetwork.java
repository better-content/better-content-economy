package com.bettercontent.spiritcommerce.resident;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ResidentNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation(BetterSpiritCommerce.MOD_ID, "resident_barter"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int id;
    private ResidentNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode).consumerMainThread(Snapshot::handle).add();
        CHANNEL.messageBuilder(Request.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode).consumerMainThread(Request::handle).add();
        CHANNEL.messageBuilder(QuoteResult.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuoteResult::encode).decoder(QuoteResult::decode).consumerMainThread(QuoteResult::handle).add();
    }

    public static void open(ServerPlayer player, LivingEntity resident) {
        send(player, resident, true, "");
    }

    private static void send(ServerPlayer player, LivingEntity resident, boolean open, String message) {
        ResidentState state = ResidentState.of(resident);
        List<ResidentBarter.Offer> offers = ResidentBarter.offers(player.serverLevel(), resident);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(resident.getUUID(),
                resident.getDisplayName().getString(), ResidentRules.needLabel(state,
                ResidentRules.hasShelter(player.serverLevel(), resident.blockPosition())), state.doing(),
                offers, open, message));
    }

    public static void request(UUID resident, ItemStack wanted, ItemStack payment, int operation) {
        CHANNEL.sendToServer(new Request(resident, wanted, payment, operation));
    }

    public record Snapshot(UUID resident, String name, String needs, String doing,
                           List<ResidentBarter.Offer> offers, boolean open, String message) {
        static void encode(Snapshot packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.resident()); buffer.writeUtf(packet.name(), 128);
            buffer.writeUtf(packet.needs(), 128); buffer.writeUtf(packet.doing(), 128);
            buffer.writeBoolean(packet.open()); buffer.writeUtf(packet.message(), 256);
            buffer.writeVarInt(packet.offers().size());
            for (ResidentBarter.Offer offer : packet.offers()) {
                buffer.writeItem(offer.result()); buffer.writeBoolean(offer.recipe() != null);
                if (offer.recipe() != null) buffer.writeResourceLocation(offer.recipe());
                buffer.writeUtf(offer.reason(), 80);
            }
        }
        static Snapshot decode(FriendlyByteBuf buffer) {
            UUID resident = buffer.readUUID(); String name = buffer.readUtf(128);
            String needs = buffer.readUtf(128); String doing = buffer.readUtf(128);
            boolean open = buffer.readBoolean(); String message = buffer.readUtf(256);
            int count = buffer.readVarInt();
            if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid offer count");
            List<ResidentBarter.Offer> offers = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack result = buffer.readItem();
                net.minecraft.resources.ResourceLocation recipe = buffer.readBoolean() ? buffer.readResourceLocation() : null;
                offers.add(new ResidentBarter.Offer(result, recipe, buffer.readUtf(80)));
            }
            return new Snapshot(resident, name, needs, doing, offers, open, message);
        }
        static void handle(Snapshot packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> ResidentClient.receive(packet));
            context.setPacketHandled(true);
        }
    }

    public record Request(UUID resident, ItemStack wanted, ItemStack payment, int operation) {
        static void encode(Request packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.resident()); buffer.writeItem(packet.wanted());
            buffer.writeItem(packet.payment()); buffer.writeVarInt(packet.operation());
        }
        static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readUUID(), buffer.readItem(), buffer.readItem(), buffer.readVarInt());
        }
        static void handle(Request packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); context.setPacketHandled(true);
            ServerPlayer player = context.getSender();
            if (player == null) return;
            context.enqueueWork(() -> process(player, packet));
        }
    }

    private static void process(ServerPlayer player, Request request) {
        Entity target = player.serverLevel().getEntity(request.resident());
        if (!(target instanceof LivingEntity resident) || !ResidentRules.isResident(resident)
                || !resident.isAlive() || player.distanceToSqr(resident) > 64) return;
        if (request.operation() == 0) { send(player, resident, false, ""); return; }
        if (request.operation() == 3) {
            if (!request.payment().isEmpty() && has(player, request.payment())
                    && ResidentState.of(resident).canAdd(request.payment())) {
                take(player, request.payment()); ResidentState.of(resident).add(request.payment());
                send(player, resident, false, "Supplies given");
            } else send(player, resident, false, "Cannot give these supplies");
            return;
        }
        ResidentBarter.Quote quote = ResidentBarter.quote(player.serverLevel(), resident,
                request.wanted(), request.payment());
        if (!quote.available() || !has(player, request.payment())) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new QuoteResult(false, quote.available() ? "Payment is no longer available" : quote.blocker()));
            return;
        }
        if (request.operation() == 1) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuoteResult(true, "You give "
                    + quote.payment().getCount() + " " + quote.payment().getHoverName().getString()
                    + " for " + quote.result().getCount() + " " + quote.result().getHoverName().getString()));
        } else if (request.operation() == 2) {
            if (!room(player, quote.result())) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuoteResult(false, "Inventory full"));
                return;
            }
            boolean success = ResidentBarter.execute(player.serverLevel(), resident, request.wanted(),
                    request.payment(), () -> take(player, request.payment()),
                    result -> player.getInventory().add(result));
            if (success) send(player, resident, false, "Trade complete");
            else CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuoteResult(false, "Trade changed; choose again"));
        }
    }

    private static boolean has(ServerPlayer player, ItemStack stack) {
        int found = 0;
        for (ItemStack slot : player.getInventory().items)
            if (ItemStack.isSameItemSameTags(slot, stack)) found += slot.getCount();
        return found >= stack.getCount();
    }

    private static boolean room(ServerPlayer player, ItemStack stack) {
        if (player.getInventory().getFreeSlot() >= 0) return true;
        for (ItemStack slot : player.getInventory().items)
            if (ItemStack.isSameItemSameTags(slot, stack)
                    && slot.getCount() + stack.getCount() <= slot.getMaxStackSize()) return true;
        return false;
    }

    private static boolean take(ServerPlayer player, ItemStack stack) {
        if (!has(player, stack)) return false;
        int needed = stack.getCount();
        for (ItemStack slot : player.getInventory().items) {
            if (!ItemStack.isSameItemSameTags(slot, stack)) continue;
            int amount = Math.min(needed, slot.getCount()); slot.shrink(amount); needed -= amount;
            if (needed == 0) break;
        }
        player.getInventory().setChanged();
        return true;
    }

    public record QuoteResult(boolean available, String message) {
        static void encode(QuoteResult packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.available()); buffer.writeUtf(packet.message(), 256);
        }
        static QuoteResult decode(FriendlyByteBuf buffer) {
            return new QuoteResult(buffer.readBoolean(), buffer.readUtf(256));
        }
        static void handle(QuoteResult packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> ResidentClient.quote(packet));
            context.setPacketHandled(true);
        }
    }
}
