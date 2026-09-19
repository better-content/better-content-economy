package com.bettercontent.economy.spirit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
final class SpiritAttributionPolicyTest {
    @Test void attributionUsesExplicitMappedEffectsAndDoesNotFollowArbitraryOwnedMobs() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bettercontent/economy/spirit/SpiritAcquisition.java"));
        assertTrue(source.contains("AreaEffectCloud cloud"));
        assertTrue(source.contains("EvokerFangs fangs"));
        assertTrue(source.contains("arbitrary owned mobs are not followed"));
    }
}
