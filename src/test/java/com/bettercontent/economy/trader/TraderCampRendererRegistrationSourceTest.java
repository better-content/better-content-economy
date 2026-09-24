package com.bettercontent.economy.trader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TraderCampRendererRegistrationSourceTest {
    @Test
    void forgeClientSubscriberIsOutsideMixinOwnedPackage() throws Exception {
        Path source = Path.of("src/main/java/com/bettercontent/economy/client/TraderCampPostRendererRegistration.java");
        String contents = Files.readString(source);

        assertTrue(contents.startsWith("package com.bettercontent.economy.client;"));
        assertTrue(contents.contains("@Mod.EventBusSubscriber"));
        assertTrue(contents.contains("@SubscribeEvent"));
        assertFalse(Files.exists(Path.of(
                "src/main/java/com/bettercontent/economy/mixin/client/TraderCampPostRendererRegistration.java")));
    }
}
