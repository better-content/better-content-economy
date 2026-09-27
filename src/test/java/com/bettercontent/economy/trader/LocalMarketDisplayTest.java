package com.bettercontent.economy.trader;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

final class LocalMarketDisplayTest {
    @BeforeAll static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            boolean expectedForgeHarnessFailure = java.util.stream.Stream.iterate((Throwable) error,
                            java.util.Objects::nonNull, Throwable::getCause)
                    .anyMatch(cause -> cause instanceof NoSuchMethodException
                            && cause.getMessage() != null
                            && cause.getMessage().startsWith("net.minecraftforge.network.NetworkEvent"));
            if (!expectedForgeHarnessFailure) throw error;
        }
    }

    private static LocalMarket.Row row() {
        return new LocalMarket.Row(UUID.randomUUID(), 4, "Lantern Trader", 1, 64, 2,
                "3 × Amethyst Shard + 2 × Copper Ingot", "1 × Lantern", "identity", 7,
                new ItemStack(Items.AMETHYST_SHARD, 3), new ItemStack(Items.COPPER_INGOT, 2),
                new ItemStack(Items.LANTERN));
    }

    @Test void searchesMerchantPaymentAndResultWithoutCaseSensitivity() {
        LocalMarket.Row row = row();
        assertTrue(LocalMarket.matches(row, " lantern "));
        assertTrue(LocalMarket.matches(row, "COPPER"));
        assertTrue(LocalMarket.matches(row, "trader"));
        assertTrue(LocalMarket.matches(row, ""));
        assertFalse(LocalMarket.matches(row, "bread"));
    }

    @Test void sortsByResultThenMerchantWithoutChangingOfferIdentity() {
        LocalMarket.Row lantern = row();
        LocalMarket.Row apple = new LocalMarket.Row(UUID.randomUUID(), 2, "Village Provisioner",
                0, 64, 0, "1 × Copper Ingot", "1 × Apple", "apple-identity", 6,
                new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY, new ItemStack(Items.APPLE));
        List<LocalMarket.Row> ordered = LocalMarket.sorted(new ArrayList<>(List.of(lantern, apple)));
        assertEquals(List.of(apple, lantern), ordered);
        assertEquals(2, ordered.get(0).offerIndex());
        assertEquals("apple-identity", ordered.get(0).identity());
    }

    @Test void snapshotPreservesDisplayStacksAndStaleStatus() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        LocalMarketNetwork.Snapshot.encode(new LocalMarketNetwork.Snapshot(List.of(row()), true), buffer);
        LocalMarketNetwork.Snapshot decoded = LocalMarketNetwork.Snapshot.decode(buffer);
        assertTrue(decoded.stale());
        assertEquals(1, decoded.rows().size());
        assertEquals(3, decoded.rows().get(0).costA().getCount());
        assertEquals(Items.COPPER_INGOT, decoded.rows().get(0).costB().getItem());
        assertEquals(Items.LANTERN, decoded.rows().get(0).resultStack().getItem());
    }

    @Test void selectedOfferPacketPreservesContainerAndIndex() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        LocalMarketNetwork.SelectedOffer.encode(new LocalMarketNetwork.SelectedOffer(12, 31), buffer);
        assertEquals(new LocalMarketNetwork.SelectedOffer(12, 31),
                LocalMarketNetwork.SelectedOffer.decode(buffer));
    }
}
