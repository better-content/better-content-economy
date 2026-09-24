package com.bettercontent.economy.ops;

import static org.junit.jupiter.api.Assertions.*;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class ObservationalEconomyDataTest {
    @Test void successfulExchangesAggregateAndRoundTripWithoutPlayerIdentity() {
        var data = new ObservationalEconomyData();
        data.recordExchange(CurrencyIdentity.WORK, 2, "minecraft:villager");
        data.recordExchange(CurrencyIdentity.WORK, 3, "minecraft:villager");
        data.recordExchange(CurrencyIdentity.TEMPO, 1, "minecraft:wandering_trader");
        var restored = ObservationalEconomyData.load(data.save(new CompoundTag()));
        assertEquals(2, restored.exchanges().size());
        assertTrue(restored.exchanges().stream().anyMatch(e -> e.identity() == CurrencyIdentity.WORK && e.paid() == 5));
        assertTrue(restored.exchanges().stream().noneMatch(e -> e.merchant().contains("uuid") || e.merchant().contains("player")));
    }

    @Test void unknownAndNewRowsAreBounded() {
        var data = new ObservationalEconomyData();
        data.recordExchange(null, 1, "minecraft:villager");
        data.recordExchange(CurrencyIdentity.WORK, 0, "minecraft:villager");
        for (int i = 0; i < 300; i++) data.recordExchange(CurrencyIdentity.WORK, 1, "merchant:" + i);
        assertEquals(256, data.exchanges().size());
    }
}
