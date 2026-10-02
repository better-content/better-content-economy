package com.bettercontent.spiritcommerce.resident;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One server-side quote and a fully preflighted transfer of physical goods. */
public final class PlayerStallTrade {
    public record Quote(boolean available, String blocker, int limit, int price, boolean crafted) {}
    private static Object cachedRecipes;
    private static final Map<Item, Boolean> CRAFTABLE = new HashMap<>();
    private PlayerStallTrade() {}

    public static Quote quote(ServerLevel level, PlayerStallBlockEntity stall, LivingEntity buyer, int index) {
        if (stall.owner() == null || index < 0 || index >= PlayerStallBlockEntity.OFFERS
                || !ResidentRules.isResident(buyer) || !buyer.isAlive()) return blocked("Unavailable");
        PlayerStallBlockEntity.Offer offer = stall.offers().get(index);
        if (!offer.enabled() || !offer.valid()) return blocked("Offer disabled");
        if (stall.stockCount(offer.sale()) < offer.sale().getCount()) return blocked("Out of stock");
        if (!ResidentBarter.wants(level, buyer, offer.sale())) return blocked("No current demand");
        ResidentBarter.PaymentPlan plan = ResidentBarter.paymentPlan(level, buyer, offer.payment());
        if (plan == null) return blocked("Buyer lacks payment or a workstation");
        if (!retainsReserve(ResidentState.of(buyer), plan.after())) return blocked("Buyer needs to keep essential supplies");
        int base = ResidentRules.basicValue(offer.sale()) * offer.sale().getCount();
        int bonus = 0;
        if (craftable(level, offer.sale())) bonus++;
        ResidentState state = ResidentState.of(buyer);
        if (state.food() < 8 && offer.sale().getFoodProperties(null) != null
                || state.water() < 8 && ResidentRules.isSafeWater(offer.sale())) bonus++;
        int limit = base * (4 + bonus) / 4;
        int price = Math.max(ResidentRules.basicValue(offer.payment()) * offer.payment().getCount(), plan.cost());
        if (price > limit) return new Quote(false, "Asking above buyer's limit", limit, price, plan.recipe() != null);
        List<ItemStack> afterBuyer = new ArrayList<>(plan.after());
        if (!ResidentBarter.addTo(afterBuyer, offer.sale())) return blocked("Buyer's inventory is full");
        if (!stall.canReceive(offer.payment())) return blocked("Payment storage is full");
        return new Quote(true, "Ready", limit, price, plan.recipe() != null);
    }

    public static boolean execute(ServerLevel level, PlayerStallBlockEntity stall, LivingEntity buyer, int index) {
        Quote quote = quote(level, stall, buyer, index);
        if (!quote.available()) return false;
        PlayerStallBlockEntity.Offer offer = stall.offers().get(index);
        ResidentBarter.PaymentPlan plan = ResidentBarter.paymentPlan(level, buyer, offer.payment());
        if (plan == null) return false;
        NonNullList<ItemStack> nextStock = PlayerStallBlockEntity.copy(stall.stock());
        NonNullList<ItemStack> nextProceeds = PlayerStallBlockEntity.copy(stall.proceeds());
        List<ItemStack> nextBuyer = new ArrayList<>(plan.after());
        if (!PlayerStallBlockEntity.remove(nextStock, offer.sale())
                || !PlayerStallBlockEntity.add(nextProceeds, offer.payment())
                || !ResidentBarter.addTo(nextBuyer, offer.sale())) return false;
        ResidentState state = ResidentState.of(buyer);
        state.replaceItems(nextBuyer);
        state.setDoing("Buying at the player stall");
        state.event("bought " + offer.sale().getCount() + " " + offer.sale().getHoverName().getString()
                + " for " + offer.payment().getCount() + " " + offer.payment().getHoverName().getString());
        stall.commit(nextStock, nextProceeds, buyer, offer, plan.recipe() != null);
        buyer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    private static Quote blocked(String reason) { return new Quote(false, reason, 0, 0, false); }

    private static boolean craftable(ServerLevel level, ItemStack item) {
        if (cachedRecipes != level.getRecipeManager()) {
            cachedRecipes = level.getRecipeManager(); CRAFTABLE.clear();
        }
        return CRAFTABLE.computeIfAbsent(item.getItem(), key -> hasRecipe(level, key));
    }

    private static boolean hasRecipe(ServerLevel level, Item item) {
        for (var recipe : level.getRecipeManager().getRecipes()) {
            ItemStack output = recipe.getResultItem(level.registryAccess());
            if (!output.isEmpty() && output.getItem() == item
                    && BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()) != null) return true;
        }
        return false;
    }

    private static boolean retainsReserve(ResidentState state, java.util.List<ItemStack> after) {
        for (ItemStack item : state.items()) {
            if (item.isEmpty()) continue;
            int reserve = ResidentRules.isSafeWater(item) && state.water() < 14 ? 2
                    : item.getFoodProperties(null) != null && state.food() < 14 ? 2
                    : item.is(net.minecraft.tags.ItemTags.LOGS) || item.is(Items.COAL)
                    || item.is(Items.CHARCOAL) || item.is(Items.GLASS_BOTTLE) ? 2 : 0;
            if (reserve > 0 && PlayerStallBlockEntity.count(after, item)
                    < Math.min(reserve, state.count(item))) return false;
        }
        return true;
    }
}
