package com.bettercontent.spiritcommerce.resident;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** BCV1 automation API for inspecting and exercising the same stall service as the GUI. */
public final class PlayerStallCommand {
    private PlayerStallCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bettervillagers").then(Commands.literal("stall")
                .then(Commands.literal("inspect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> inspect(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))))
                .then(Commands.literal("quote").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .then(Commands.argument("offer", IntegerArgumentType.integer(0, 5))
                                        .executes(c -> quote(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                                StringArgumentType.getString(c, "resident"),
                                                IntegerArgumentType.getInteger(c, "offer"), false))))))
                .then(Commands.literal("execute").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .then(Commands.argument("offer", IntegerArgumentType.integer(0, 5))
                                        .executes(c -> quote(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                                StringArgumentType.getString(c, "resident"),
                                                IntegerArgumentType.getInteger(c, "offer"), true))))))
                .then(Commands.literal("configure").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("offer", IntegerArgumentType.integer(0, 5))
                                .then(Commands.argument("sale", ResourceLocationArgument.id())
                                        .then(Commands.argument("sale_count", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("payment", ResourceLocationArgument.id())
                                                        .then(Commands.argument("payment_count", IntegerArgumentType.integer(1, 64))
                                                                .executes(c -> configure(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                                                        IntegerArgumentType.getInteger(c, "offer"),
                                                                        ResourceLocationArgument.getId(c, "sale"),
                                                                        IntegerArgumentType.getInteger(c, "sale_count"),
                                                                        ResourceLocationArgument.getId(c, "payment"),
                                                                        IntegerArgumentType.getInteger(c, "payment_count"))))))))))
                .then(Commands.literal("clear").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("offer", IntegerArgumentType.integer(0, 5))
                                .executes(c -> clear(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                        IntegerArgumentType.getInteger(c, "offer"))))))
                .then(Commands.literal("seed").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> seed(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                                        ResourceLocationArgument.getId(c, "item"),
                                                        IntegerArgumentType.getInteger(c, "count")))))))
                .then(Commands.literal("deposit").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("inventory_slot", IntegerArgumentType.integer(0, 35))
                                .executes(c -> deposit(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                        IntegerArgumentType.getInteger(c, "inventory_slot"))))))
                .then(Commands.literal("withdraw").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 17))
                                        .executes(c -> withdraw(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"),
                                                StringArgumentType.getString(c, "kind"),
                                                IntegerArgumentType.getInteger(c, "slot")))))))));
    }

    private static PlayerStallBlockEntity stall(CommandSourceStack source, BlockPos pos) {
        return source.getLevel().getBlockEntity(pos) instanceof PlayerStallBlockEntity stall ? stall : null;
    }

    private static int inspect(CommandSourceStack source, BlockPos pos) {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        JsonObject json = base(pos);
        json.addProperty("owner", stall.owner() == null ? "" : stall.owner().toString());
        json.addProperty("visiting", stall.visiting());
        json.add("stock", stacks(stall.stock())); json.add("proceeds", stacks(stall.proceeds()));
        JsonArray offers = new JsonArray();
        for (PlayerStallBlockEntity.Offer offer : stall.offers()) {
            JsonObject row = new JsonObject();
            row.add("sale", stack(offer.sale())); row.add("payment", stack(offer.payment()));
            row.addProperty("enabled", offer.enabled()); offers.add(row);
        }
        json.add("offers", offers);
        JsonArray events = new JsonArray();
        for (String line : stall.events()) events.add(line);
        json.add("events", events);
        return emit(source, json);
    }

    private static int quote(CommandSourceStack source, BlockPos pos, String raw, int offer, boolean execute) {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        LivingEntity buyer = resident(source, raw);
        if (buyer == null || buyer.level() != source.getLevel()) return error(source, "unknown_resident");
        PlayerStallTrade.Quote result = PlayerStallTrade.quote(source.getLevel(), stall, buyer, offer);
        JsonObject json = base(pos);
        json.addProperty("resident", buyer.getUUID().toString()); json.addProperty("offer", offer);
        json.addProperty("available", result.available()); json.addProperty("limit", result.limit());
        json.addProperty("price", result.price()); json.addProperty("crafted", result.crafted());
        if (!result.available()) json.addProperty("blocker", result.blocker());
        if (execute) {
            BlockPos front = pos.relative(stall.getBlockState().getValue(PlayerStallBlock.FACING));
            if (!authorized(source, stall, pos) || buyer.blockPosition().distSqr(front) > 4)
                return error(source, "not_authorized_or_too_far");
            json.addProperty("executed", result.available() && PlayerStallTrade.execute(source.getLevel(), stall, buyer, offer));
        }
        return emit(source, json);
    }

    private static LivingEntity resident(CommandSourceStack source, String raw) {
        try {
            Entity entity = source.getLevel().getEntity(UUID.fromString(raw));
            if (entity instanceof LivingEntity living && ResidentRules.isResident(living)) return living;
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    private static int configure(CommandSourceStack source, BlockPos pos, int offer, ResourceLocation saleId,
                                 int saleCount, ResourceLocation paymentId, int paymentCount) {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        if (!authorized(source, stall, pos)) return error(source, "not_authorized");
        ItemStack sale = item(saleId, saleCount), payment = item(paymentId, paymentCount);
        if (sale.isEmpty() || payment.isEmpty()) return error(source, "invalid_item_or_count");
        if (!stall.configure(offer, sale, payment, true)) return error(source, "invalid_offer");
        return inspect(source, pos);
    }

    private static int clear(CommandSourceStack source, BlockPos pos, int offer) {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        if (!authorized(source, stall, pos)) return error(source, "not_authorized");
        stall.configure(offer, ItemStack.EMPTY, ItemStack.EMPTY, false);
        return inspect(source, pos);
    }

    private static int seed(CommandSourceStack source, BlockPos pos, ResourceLocation id, int count) {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        ItemStack item = item(id, count);
        if (item.isEmpty() || !stall.seedStock(item)) return error(source, "cannot_seed");
        return inspect(source, pos);
    }

    private static int deposit(CommandSourceStack source, BlockPos pos, int slot) throws CommandSyntaxException {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        ServerPlayer player = source.getPlayerOrException();
        if (!authorized(source, stall, pos) || !stall.deposit(player, slot)) return error(source, "cannot_deposit");
        return inspect(source, pos);
    }

    private static int withdraw(CommandSourceStack source, BlockPos pos, String kind, int slot)
            throws CommandSyntaxException {
        PlayerStallBlockEntity stall = stall(source, pos);
        if (stall == null) return error(source, "unknown_stall");
        ServerPlayer player = source.getPlayerOrException();
        if (!authorized(source, stall, pos) || !stall.owns(player)
                || !(kind.equals("stock") || kind.equals("proceeds"))
                || !stall.withdraw(player, kind.equals("proceeds"), slot)) return error(source, "cannot_withdraw");
        return inspect(source, pos);
    }

    private static boolean authorized(CommandSourceStack source, PlayerStallBlockEntity stall, BlockPos pos) {
        if (source.hasPermission(2)) return true;
        try {
            ServerPlayer player = source.getPlayerOrException();
            return stall.owns(player) && player.blockPosition().distSqr(pos) <= 64;
        } catch (CommandSyntaxException ignored) { return false; }
    }

    private static ItemStack item(ResourceLocation id, int count) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR || count > item.getMaxStackSize()) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    private static JsonObject base(BlockPos pos) {
        JsonObject json = new JsonObject();
        json.addProperty("version", "bc.villagers.v1");
        json.addProperty("stall", pos.toShortString()); return json;
    }

    private static JsonArray stacks(java.util.List<ItemStack> items) {
        JsonArray rows = new JsonArray();
        for (int i = 0; i < items.size(); i++) if (!items.get(i).isEmpty()) {
            JsonObject row = stack(items.get(i)); row.addProperty("slot", i); rows.add(row);
        }
        return rows;
    }

    private static JsonObject stack(ItemStack item) {
        JsonObject row = new JsonObject();
        row.addProperty("item", item.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        row.addProperty("count", item.getCount());
        if (item.hasTag()) row.addProperty("tag", item.getTag().toString());
        return row;
    }

    private static int error(CommandSourceStack source, String code) {
        JsonObject json = new JsonObject(); json.addProperty("version", "bc.villagers.v1");
        json.addProperty("error", code); return emit(source, json);
    }

    private static int emit(CommandSourceStack source, JsonObject json) {
        source.sendSuccess(() -> Component.literal("BCV1 " + json), false); return json.has("error") ? 0 : 1;
    }
}
