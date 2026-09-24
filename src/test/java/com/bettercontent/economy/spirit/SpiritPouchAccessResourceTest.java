package com.bettercontent.economy.spirit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SpiritPouchAccessResourceTest {
    @Test
    void accessUsesTheEquippedBeltPouchAndNativeMalumMenu() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/com/bettercontent/economy/spirit/SpiritPouchAccess.java"));
        final String belt = Files.readString(Path.of("src/main/resources/data/curios/tags/items/belt.json"));
        assertTrue(belt.contains("malum:spirit_pouch"));
        assertTrue(source.contains("stack -> stack.is(pouch)"));
        assertTrue(source.contains("player.getInventory().items.stream()"));
        assertTrue(source.contains("findFirst().orElse(ItemStack.EMPTY)"));
        assertTrue(source.contains("!result.slotContext().cosmetic()"));
        assertTrue(source.contains("result.slotContext().entity() == player"));
        assertTrue(source.contains("new SpiritPouchContainer"));
        assertTrue(source.contains("buffer.writeItem(stack)"));
    }

    @Test
    void currencySlotMixinIsRegisteredAndOnlyAcceptsEconomyCurrencies() throws Exception {
        final String mixin = Files.readString(Path.of(
                "src/main/java/com/bettercontent/economy/mixin/SpiritPouchCurrencySlotMixin.java"));
        final String config = Files.readString(Path.of("src/main/resources/better_content_economy.mixins.json"));

        assertTrue(config.contains("SpiritPouchCurrencySlotMixin"));
        assertTrue(mixin.contains("@Mixin(targets = \"com.sammy.malum.common.container.SpiritPouchContainer$1\""));
        assertTrue(mixin.contains("@Inject(method = \"mayPlace\""));
        assertTrue(mixin.contains("CurrencyIdentity.fromItemId(ForgeRegistries.ITEMS.getKey(stack.getItem())) != null"));
        assertTrue(mixin.contains("callback.setReturnValue(true)"));
    }
}
