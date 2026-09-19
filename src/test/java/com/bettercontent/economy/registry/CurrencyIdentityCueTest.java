package com.bettercontent.economy.registry;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
final class CurrencyIdentityCueTest {
    @Test void everyOrdinaryIdentityHasTextCueAndDedicatedItemImplementation() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/better_content_economy/lang/en_us.json"));
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/economy/registry/CurrencyItems.java"));
        for (CurrencyIdentity identity : CurrencyIdentity.values()) assertTrue(lang.contains("item.better_content_economy." + identity.id() + "_spirit"));
        assertTrue(source.contains("new CurrencyItem(identity)"));
        String item = Files.readString(Path.of("src/main/java/com/bettercontent/economy/registry/CurrencyItem.java"));
        assertTrue(item.contains("identity.id()"));
        for (CurrencyIdentity identity : CurrencyIdentity.values()) assertTrue(lang.contains("tooltip.better_content_economy." + identity.id()));
        var cues = new HashSet<String>();
        for (String line : lang.lines().toList()) if (line.contains("tooltip.better_content_economy.")) cues.add(line.substring(line.indexOf(":") + 1).trim());
        assertTrue(cues.size() >= CurrencyIdentity.values().length);
    }
}
