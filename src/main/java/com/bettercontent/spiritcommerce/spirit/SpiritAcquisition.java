package com.bettercontent.spiritcommerce.spirit;

import com.bettercontent.spiritcommerce.BetterSpiritCommerce;
import com.bettercontent.spiritcommerce.config.EconomyPolicy;
import com.bettercontent.spiritcommerce.ops.ObservationalEconomyData;
import com.bettercontent.spiritcommerce.registry.CurrencyItems;
import com.mojang.logging.LogUtils;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.core.handlers.SpiritHarvestHandler;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.registry.common.SoundRegistry;
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
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Releases physical Better Content spirits from the credited victim at death. */
public final class SpiritAcquisition {
    public static final String GROUPED_PICKUP_MARKER = "BetterContentGroupedSpiritPickup";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SPIRITLESS_ACTORS =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(BetterSpiritCommerce.MOD_ID, "spiritless_economy_actors"));

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
            // Release each type as one Malum spirit carrying its full stack. Malum's
            // per-unit helper makes the same kill arrive in a long train of pickups.
            releaseGroupedSpirits(physicalDrops, victim, recipient);
        }
        if (!credits.isEmpty()) {
            ObservationalEconomyData observations = ObservationalEconomyData.get(recipient.server.overworld());
            observations.recordRegion(regionIdentity(victim).key(), credits);
            observations.recordRelease(credits);
            observations.recordActivity();
        }
        capability.soulData.soulless = true;
    }

    static void releaseGroupedSpirits(final List<ItemStack> drops, final LivingEntity victim,
                                      final ServerPlayer recipient) {
        Level level = victim.level();
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() / 2.0;
        double z = victim.getZ();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            double vx = (level.random.nextDouble() - 0.5) * 0.3;
            double vz = (level.random.nextDouble() - 0.5) * 0.3;
            ItemStack grouped = stack.copy();
            // Malum represents an untagged native spirit as a single default item even when
            // given a larger count. This marker forces its entity to retain the whole stack.
            grouped.getOrCreateTag().putBoolean(GROUPED_PICKUP_MARKER, true);
            level.addFreshEntity(new SpiritItemEntity(level, recipient.getUUID(), grouped,
                    x, y, z, vx, 0.055, vz));
        }
        level.playSound(null, x, y, z, SoundRegistry.SOUL_SHATTER.get(), SoundSource.PLAYERS,
                1.0F, 0.7F + level.random.nextFloat() * 0.4F);
    }

    public static void removeGroupedPickupMarker(ItemStack stack) {
        if (stack.getTag() == null || !stack.getTag().getBoolean(GROUPED_PICKUP_MARKER)) return;
        stack.getTag().remove(GROUPED_PICKUP_MARKER);
        if (stack.getTag().isEmpty()) stack.setTag(null);
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
