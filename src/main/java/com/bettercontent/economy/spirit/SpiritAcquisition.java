package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.ops.ObservationalEconomyData;
import com.bettercontent.economy.registry.CurrencyItems;
import com.mojang.logging.LogUtils;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.core.handlers.SpiritHarvestHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Releases physical Better Content spirits from the credited victim at death. */
public final class SpiritAcquisition {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SPIRITLESS_ACTORS =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(BetterContentEconomy.MOD_ID, "spiritless_economy_actors"));

    private SpiritAcquisition() {}

    // Claim the death before Malum's own exposed-soul listener can emit a second drop.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(final LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (event.isCanceled() || level.isClientSide || victim instanceof ServerPlayer || isEconomyActor(victim)) return;
        ServerPlayer recipient = creditedPlayer(event.getSource().getEntity());
        if (recipient == null) return;

        var capability = MalumLivingEntityDataCapability.getCapability(victim);
        if (capability.soulData.spawnerSpawned || capability.soulData.soulless) return;

        List<ItemStack> nativeDrops = SpiritHarvestHandler.getSpawnedSpirits(victim, recipient, ItemStack.EMPTY);
        if (nativeDrops.isEmpty() && (victim instanceof Enemy
                || victim.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            int seed = id == null ? victim.getType().hashCode() : id.toString().hashCode();
            nativeDrops = java.util.stream.IntStream.range(0, EconomyPolicy.acquisition().unmappedHostileSpiritCount())
                    .mapToObj(offset -> spirit(SpiritKind.fromIndex(seed + offset * 3)))
                    .filter(stack -> !stack.isEmpty()).toList();
            LOGGER.error("Hostile entity {} has no Malum spirit mapping; using deterministic two-spirit fallback", id);
        }

        Map<CurrencyIdentity, Integer> credits = SpiritCreditAllocation.fromNative(nativeDrops,
                victim.getUUID(), recipient.getUUID(), regionIdentity(victim).seed());
        List<ItemStack> physicalDrops = new ArrayList<>();
        for (var entry : credits.entrySet()) {
            if (entry.getValue() > 0) physicalDrops.add(new ItemStack(CurrencyItems.item(entry.getKey()).get(), entry.getValue()));
        }
        for (ItemStack stack : nativeDrops) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (CurrencyIdentity.fromLegacyNativeSpirit(id) == null && CurrencyIdentity.fromItemId(id) == null) {
                physicalDrops.add(stack.copy());
            }
        }
        if (!physicalDrops.isEmpty()) {
            // Malum emits one owned floating spirit per unit at the victim and honors its
            // NO_FANCY_SPIRITS setting. Untagged items combine into ordinary 64-item stacks.
            SpiritHarvestHandler.spawnItemsAsSpirits(physicalDrops, victim, recipient);
        }
        if (!credits.isEmpty()) {
            ObservationalEconomyData observations = ObservationalEconomyData.get(recipient.server.overworld());
            observations.recordRegion(regionIdentity(victim).key(), credits);
            observations.recordRelease(credits);
            observations.recordActivity();
        }
        capability.soulData.soulless = true;
    }

    private static boolean isEconomyActor(final LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof IronGolem || entity.getType().is(SPIRITLESS_ACTORS);
    }

    private static ServerPlayer creditedPlayer(final Entity source) { return playerSource(source); }

    private static SpiritRegionIdentity regionIdentity(final LivingEntity victim) {
        var level = victim.level();
        var biome = level.getBiome(victim.blockPosition()).unwrapKey();
        return SpiritRegionIdentity.of(level.dimension().location().toString(),
                biome.map(key -> key.location().toString()).orElse("unknown"));
    }

    // Only explicit player-owned effects count; arbitrary owned mobs are not followed.
    private static ServerPlayer playerSource(final Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) return player;
        if (entity instanceof AreaEffectCloud cloud && cloud.getOwner() instanceof ServerPlayer player) return player;
        if (entity instanceof EvokerFangs fangs && fangs.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    private static ItemStack spirit(final SpiritKind kind) {
        Item item = ForgeRegistries.ITEMS.getValue(kind.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
