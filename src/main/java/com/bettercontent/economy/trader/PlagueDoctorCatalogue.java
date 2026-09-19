package com.bettercontent.economy.trader;

import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.spirit.SpiritKind;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import com.bettercontent.economy.registry.CurrencyItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

/** A daily cabinet of eight strange, mixed-spirit specimens. */
public final class PlagueDoctorCatalogue {
    static final ResourceLocation PLAGUE_DOCTOR = new ResourceLocation("rats", "plague_doctor");
    static final String VERSION_TAG = "better_content_economy.plague_catalogue_version";
    static final String DAY_TAG = "better_content_economy.plague_catalogue_day";
    private static final int VERSION = 1;
    private static final int DAILY_OFFERS = 8;

    private PlagueDoctorCatalogue() {}

    public static boolean isPlagueDoctor(final Entity entity) {
        return PLAGUE_DOCTOR.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    public static void ensureOffers(final AbstractVillager doctor, final MerchantOffers offers) {
        if (!(doctor.level() instanceof ServerLevel level)) return;
        long day = Math.floorDiv(level.getServer().overworld().getDayTime(), 24_000L);
        ensureOffers(doctor, offers, day);
    }

    static void ensureOffers(final AbstractVillager doctor, final MerchantOffers offers, final long day) {
        var data = doctor.getPersistentData();
        boolean current = data.getInt(VERSION_TAG) == VERSION && data.getLong(DAY_TAG) == day;
        if (current && !offers.isEmpty()) return;
        if (doctor.getTradingPlayer() != null) return;

        List<EconomyPolicy.PlagueDoctorRow> selected = selectRows(doctor.getUUID(), day,
                id -> item(id) != Items.AIR);
        offers.clear();
        for (EconomyPolicy.PlagueDoctorRow row : selected) {
            Item payment = CurrencyItems.item(row.kind().currencyIdentity()).get();
            Item result = item(new ResourceLocation(row.result().id()));
            if (payment == Items.AIR || result == Items.AIR) continue;
            offers.add(new MerchantOffer(new ItemStack(payment, row.cost()),
                    new ItemStack(result, row.result().count()), row.maxUses(), row.xp(), 0.0F));
        }
        data.putInt(VERSION_TAG, VERSION);
        data.putLong(DAY_TAG, day);
    }

    static List<EconomyPolicy.PlagueDoctorRow> selectRows(final UUID doctorId, final long day,
                                                           final Predicate<ResourceLocation> available) {
        List<EconomyPolicy.PlagueDoctorRow> candidates = new ArrayList<>();
        for (EconomyPolicy.PlagueDoctorRow row : EconomyPolicy.plagueDoctorRows()) {
            ResourceLocation result = new ResourceLocation(row.result().id());
            CurrencyIdentity currency = row.kind().currencyIdentity();
            if (currency != null && available.test(currency.itemId()) && available.test(result)) candidates.add(row);
        }
        long seed = doctorId.getMostSignificantBits()
                ^ Long.rotateLeft(doctorId.getLeastSignificantBits(), 17)
                ^ day * 0x9E3779B97F4A7C15L;
        RandomSource random = RandomSource.create(seed);
        for (int index = candidates.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            EconomyPolicy.PlagueDoctorRow held = candidates.get(index);
            candidates.set(index, candidates.get(swap));
            candidates.set(swap, held);
        }
        return List.copyOf(candidates.subList(0, Math.min(DAILY_OFFERS, candidates.size())));
    }

    private static Item item(final ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Items.AIR : item;
    }
}
