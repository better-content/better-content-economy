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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Machine readable console interface; every answer is one BCV1 JSON line. */
public final class ResidentCommand {
    private ResidentCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bettervillagers")
                .then(Commands.literal("inspect")
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .executes(context -> inspect(context.getSource(),
                                        StringArgumentType.getString(context, "resident")))))
                .then(Commands.literal("offers")
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .executes(context -> offers(context.getSource(),
                                        StringArgumentType.getString(context, "resident")))))
                .then(Commands.literal("events")
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .executes(context -> events(context.getSource(),
                                        StringArgumentType.getString(context, "resident")))))
                .then(Commands.literal("quote")
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .then(Commands.argument("result", ResourceLocationArgument.id())
                                        .then(Commands.argument("result_count", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("payment", ResourceLocationArgument.id())
                                                        .then(Commands.argument("payment_count", IntegerArgumentType.integer(1, 64))
                                                        .executes(context -> quote(context.getSource(),
                                                                StringArgumentType.getString(context, "resident"),
                                                                ResourceLocationArgument.getId(context, "result").toString(),
                                                                ResourceLocationArgument.getId(context, "payment").toString(),
                                                                IntegerArgumentType.getInteger(context, "result_count"),
                                                                IntegerArgumentType.getInteger(context, "payment_count")))))))))
                .then(Commands.literal("execute")
                        .then(Commands.argument("resident", StringArgumentType.word())
                                .then(Commands.argument("result", ResourceLocationArgument.id())
                                        .then(Commands.argument("result_count", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("payment", ResourceLocationArgument.id())
                                                        .then(Commands.argument("payment_count", IntegerArgumentType.integer(1, 64))
                                                        .executes(context -> execute(context.getSource(),
                                                                StringArgumentType.getString(context, "resident"),
                                                                ResourceLocationArgument.getId(context, "result").toString(),
                                                                ResourceLocationArgument.getId(context, "payment").toString(),
                                                                IntegerArgumentType.getInteger(context, "result_count"),
                                                                IntegerArgumentType.getInteger(context, "payment_count"))))))))));
    }

    private static LivingEntity resident(CommandSourceStack source, String raw) {
        try {
            UUID id = UUID.fromString(raw);
            for (ServerLevel level : source.getServer().getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity instanceof LivingEntity living && ResidentRules.isResident(living)) return living;
            }
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    private static int inspect(CommandSourceStack source, String raw) {
        LivingEntity resident = resident(source, raw);
        if (resident == null) return error(source, "unknown_resident");
        ResidentState state = ResidentState.of(resident);
        JsonObject json = base(resident);
        json.addProperty("food", state.food()); json.addProperty("water", state.water());
        json.addProperty("rest", state.rest()); json.addProperty("doing", state.doing());
        json.addProperty("shelter", ResidentRules.hasShelter((ServerLevel) resident.level(), resident.blockPosition()));
        JsonArray inventory = new JsonArray();
        for (ItemStack item : state.items()) if (!item.isEmpty()) inventory.add(stack(item));
        json.add("inventory", inventory);
        return emit(source, json);
    }

    private static int offers(CommandSourceStack source, String raw) {
        LivingEntity resident = resident(source, raw);
        if (resident == null) return error(source, "unknown_resident");
        JsonObject json = base(resident);
        JsonArray offers = new JsonArray();
        for (ResidentBarter.Offer offer : ResidentBarter.offers((ServerLevel) resident.level(), resident)) {
            JsonObject row = stack(offer.result());
            row.addProperty("source", offer.recipe() == null ? "stock" : offer.recipe().toString());
            offers.add(row);
        }
        json.add("offers", offers);
        return emit(source, json);
    }

    private static int events(CommandSourceStack source, String raw) {
        LivingEntity resident = resident(source, raw);
        if (resident == null) return error(source, "unknown_resident");
        JsonObject json = base(resident);
        JsonArray events = new JsonArray();
        for (String event : ResidentState.of(resident).events()) events.add(event);
        json.add("events", events);
        return emit(source, json);
    }

    private static int quote(CommandSourceStack source, String raw, String result, String payment,
                             int resultCount, int paymentCount) {
        LivingEntity resident = resident(source, raw);
        if (resident == null) return error(source, "unknown_resident");
        Item resultItem = BuiltInRegistries.ITEM.get(new ResourceLocation(result));
        Item paymentItem = BuiltInRegistries.ITEM.get(new ResourceLocation(payment));
        if (resultItem == Items.AIR || paymentItem == Items.AIR) return error(source, "unknown_item");
        ResidentBarter.Quote quote = ResidentBarter.quote((ServerLevel) resident.level(), resident,
                new ItemStack(resultItem, resultCount), new ItemStack(paymentItem, paymentCount));
        JsonObject json = base(resident);
        json.addProperty("available", quote.available());
        json.add("result", stack(quote.result())); json.add("payment", stack(quote.payment()));
        if (quote.blocker() != null) json.addProperty("blocker", quote.blocker());
        if (quote.recipe() != null) json.addProperty("recipe", quote.recipe().toString());
        return emit(source, json);
    }

    private static int execute(CommandSourceStack source, String raw, String result, String payment,
                               int resultCount, int paymentCount)
            throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        LivingEntity resident = resident(source, raw);
        if (resident == null) return error(source, "unknown_resident");
        if (resident.level() != player.level() || player.distanceToSqr(resident) > 64)
            return error(source, "too_far");
        Item resultItem = BuiltInRegistries.ITEM.get(new ResourceLocation(result));
        Item paymentItem = BuiltInRegistries.ITEM.get(new ResourceLocation(payment));
        if (resultItem == Items.AIR || paymentItem == Items.AIR) return error(source, "unknown_item");
        ItemStack price = new ItemStack(paymentItem, paymentCount);
        int available = 0;
        for (ItemStack slot : player.getInventory().items)
            if (ItemStack.isSameItemSameTags(slot, price)) available += slot.getCount();
        if (available < price.getCount()) return error(source, "payment_missing");
        ItemStack wanted = new ItemStack(resultItem, resultCount);
        ResidentBarter.Quote quote = ResidentBarter.quote(player.serverLevel(), resident, wanted, price);
        if (!quote.available()) return error(source, quote.blocker());
        boolean room = player.getInventory().getFreeSlot() >= 0;
        if (!room) for (ItemStack slot : player.getInventory().items)
            if (ItemStack.isSameItemSameTags(slot, quote.result())
                    && slot.getCount() + quote.result().getCount() <= slot.getMaxStackSize()) { room = true; break; }
        if (!room) return error(source, "inventory_full");
        boolean completed = ResidentBarter.execute(player.serverLevel(), resident, wanted, price,
                () -> player.getInventory().clearOrCountMatchingItems(s -> ItemStack.isSameItemSameTags(s, price),
                        price.getCount(), player.getInventory()) == price.getCount(),
                item -> player.getInventory().add(item));
        if (!completed) return error(source, "stale_quote");
        JsonObject json = base(resident);
        json.addProperty("status", "completed");
        json.add("result", stack(quote.result()));
        json.add("payment", stack(price));
        return emit(source, json);
    }

    private static JsonObject base(LivingEntity resident) {
        JsonObject json = new JsonObject();
        json.addProperty("version", "bc.villagers.v1");
        json.addProperty("resident", resident.getUUID().toString());
        json.addProperty("dimension", resident.level().dimension().location().toString());
        json.addProperty("name", resident.getDisplayName().getString());
        return json;
    }

    private static JsonObject stack(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());
        if (stack.hasTag()) json.addProperty("nbt", stack.getTag().toString());
        return json;
    }

    private static int error(CommandSourceStack source, String error) {
        JsonObject json = new JsonObject(); json.addProperty("version", "bc.villagers.v1");
        json.addProperty("error", error);
        emit(source, json); return 0;
    }

    private static int emit(CommandSourceStack source, JsonObject json) {
        source.sendSuccess(() -> Component.literal("BCV1 " + json), false);
        return 1;
    }
}
