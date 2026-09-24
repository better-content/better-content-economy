package com.bettercontent.economy.trader;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.SavedData;

/** Shared finite authored merchant inventory saved with the overworld. */
public final class AuthoredStockData extends SavedData {
    private static final String DATA_NAME = "better_content_economy_authored_stock";
    private static final String STOCK_TAG = "stock";
    private static final String VERSION_TAG = "version";
    private static final int VERSION = 2;

    private final AuthoredStockLedger ledger;

    private AuthoredStockData() {
        ledger = new AuthoredStockLedger();
        AuthoredStockPolicy.initialStockUnits().forEach(ledger::seed);
        setDirty();
    }

    private AuthoredStockData(final AuthoredStockLedger ledger) {
        this.ledger = ledger;
    }

    public static AuthoredStockData get(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                AuthoredStockData::load, AuthoredStockData::new, DATA_NAME);
    }

    public static AuthoredStockData load(final CompoundTag tag) {
        int version = tag.getInt(VERSION_TAG);
        if (version == 1) {
            AuthoredStockLedger migrated = AuthoredStockLedger.migrateOfferKeys(tag.getCompound(STOCK_TAG));
            return new AuthoredStockData(migrated);
        }
        if (version != VERSION) {
            throw new IllegalStateException("Unsupported authored merchant stock data version "
                    + version);
        }
        return new AuthoredStockData(AuthoredStockLedger.load(tag.getCompound(STOCK_TAG)));
    }

    static AuthoredStockData createForTest(final Map<String, Integer> stock) {
        AuthoredStockLedger ledger = new AuthoredStockLedger();
        stock.forEach(ledger::seed);
        return new AuthoredStockData(ledger);
    }

    public boolean canPurchase(final MerchantOffer offer) {
        if (!AuthoredStockPolicy.isFinite(offer)) return true;
        return canPurchase(AuthoredOfferStock.key(offer), offer.getResult().getCount());
    }

    public boolean recordPurchase(final MerchantOffer offer) {
        if (!AuthoredStockPolicy.isFinite(offer)) return true;
        return recordPurchase(AuthoredOfferStock.key(offer), offer.getResult().getCount());
    }

    boolean canPurchase(final String offer, final int outputUnits) {
        if (!ledger.contains(offer)) return true;
        return outputUnits > 0 && ledger.remaining(offer) >= outputUnits;
    }

    boolean recordPurchase(final String offer, final int outputUnits) {
        if (!ledger.contains(offer)) return true;
        boolean purchased = outputUnits > 0 && ledger.purchase(offer, outputUnits);
        if (purchased) setDirty();
        return purchased;
    }

    int remaining(final String offer) {
        return ledger.remaining(offer);
    }

    int remainingUnits(final MerchantOffer offer) {
        String key = AuthoredOfferStock.key(offer);
        return ledger.contains(key) ? ledger.remaining(key) : Integer.MAX_VALUE;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putInt(VERSION_TAG, VERSION);
        tag.put(STOCK_TAG, ledger.save());
        return tag;
    }
}
