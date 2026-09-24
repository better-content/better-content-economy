package com.bettercontent.economy.trader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Server-side view and validation for the nearby Local Market. */
public final class LocalMarket {
    public static final double RANGE = 8.0D;
    private LocalMarket() {}

    public record Row(UUID merchant, int offerIndex, String merchantName, int x, int y, int z, String payment,
                      String result, String identity, int usesRemaining) {}

    public static List<Row> snapshot(ServerPlayer player) {
        List<Row> rows = new ArrayList<>();
        for (AbstractVillager merchant : player.serverLevel().getEntitiesOfClass(AbstractVillager.class,
                player.getBoundingBox().inflate(RANGE), Entity::isAlive)) {
            if (merchant.isRemoved() || merchant.distanceToSqr(player) > RANGE * RANGE) continue;
            MerchantOffers offers = merchant.getOffers();
            for (int index = 0; index < offers.size(); index++) {
                if (rows.size() == 256) return List.copyOf(rows);
                MerchantOffer offer = offers.get(index);
                if (offer.isOutOfStock() || offer.getUses() >= offer.getMaxUses()) continue;
                int nativeRemaining = Math.max(0, offer.getMaxUses() - offer.getUses());
                int availableTrades = AuthoredMerchantStock.availableTrades(
                        merchant, offer, player.serverLevel(), nativeRemaining);
                if (availableTrades <= 0) continue;
                var pos = merchant.blockPosition();
                rows.add(new Row(merchant.getUUID(), index, bounded(merchant.getDisplayName().getString(), 128),
                        pos.getX(), pos.getY(), pos.getZ(),
                        bounded(paymentLabel(offer), 256), bounded(itemLabel(offer.getResult()), 256), identity(offer),
                        availableTrades));
            }
        }
        return List.copyOf(rows);
    }

    public static boolean validSelection(ServerPlayer player, UUID merchantId, int offerIndex, String expectedIdentity) {
        if (player == null || merchantId == null || offerIndex < 0 || offerIndex > 255
                || expectedIdentity == null || expectedIdentity.length() > 512) return false;
        Entity entity = player.serverLevel().getEntity(merchantId);
        if (!(entity instanceof AbstractVillager merchant)) return false;
        MerchantOffers offers = merchant.getOffers();
        boolean hasOffer = offerIndex >= 0 && offerIndex < offers.size();
        MerchantOffer offer = hasOffer ? offers.get(offerIndex) : null;
        boolean hasFiniteStock = offer != null
                && AuthoredMerchantStock.canPickup(merchant, offer, player.serverLevel());
        return accepts(true, merchant.isAlive() && !merchant.isRemoved(), merchant.level() == player.serverLevel(),
                merchant.distanceToSqr(player), offerIndex, hasOffer, offer != null && !offer.isOutOfStock(),
                hasFiniteStock, offer == null ? 0 : offer.getUses(), offer == null ? 0 : offer.getMaxUses(),
                offer == null ? "" : identity(offer), expectedIdentity);
    }

    static boolean accepts(boolean merchant, boolean alive, boolean sameDimension, double distanceSquared,
                           int index, boolean offerExists, boolean inStock, boolean finiteStockAvailable,
                           int uses, int maxUses,
                           String currentIdentity, String expectedIdentity) {
        return merchant && alive && sameDimension && distanceSquared <= RANGE * RANGE
                && index >= 0 && index <= 255 && offerExists && inStock && finiteStockAvailable
                && maxUses > 0 && uses < maxUses
                && expectedIdentity != null && currentIdentity.equals(expectedIdentity);
    }

    public static void openSelected(ServerPlayer player, UUID merchantId, int offerIndex, String expectedIdentity) {
        if (!validSelection(player, merchantId, offerIndex, expectedIdentity)) return;
        AbstractVillager merchant = (AbstractVillager) player.serverLevel().getEntity(merchantId);
        int level = merchant instanceof Villager villager ? villager.getVillagerData().getLevel() : 1;
        merchant.openTradingScreen(player, merchant.getDisplayName(), level);
    }

    static String identity(MerchantOffer offer) {
        String value = stackIdentity(offer.getBaseCostA()) + '|' + stackIdentity(offer.getCostB()) + '|'
                + stackIdentity(offer.getCostA()) + '|' + stackIdentity(offer.getResult()) + '|'
                + offer.getUses() + '|' + offer.getMaxUses() + '|' + offer.getSpecialPriceDiff() + '|'
                + offer.getDemand() + '|' + offer.getPriceMultiplier();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String stackIdentity(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + "*" + stack.getCount() + "*" + stack.getTag();
    }

    private static String paymentLabel(MerchantOffer offer) {
        String first = itemLabel(offer.getCostA());
        if (offer.getCostB().isEmpty()) return first;
        return first + " + " + itemLabel(offer.getCostB());
    }

    private static String itemLabel(ItemStack stack) {
        return stack.getCount() + " × " + stack.getHoverName().getString();
    }

    private static String bounded(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
