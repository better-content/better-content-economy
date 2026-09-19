package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.config.EconomyPolicy;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.core.handlers.SpiritHarvestHandler;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Makes native Malum spirit release a reward for credited player kills, independent of equipment. */
public final class SpiritAcquisition {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SPIRITLESS_ACTORS =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(BetterContentEconomy.MOD_ID, "spiritless_economy_actors"));

    private SpiritAcquisition() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(final LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (event.isCanceled() || level.isClientSide || victim instanceof ServerPlayer || isEconomyActor(victim)) return;

        ServerPlayer recipient = creditedPlayer(event.getSource().getEntity(), victim.getKillCredit());
        if (recipient == null) return;

        var capability = MalumLivingEntityDataCapability.getCapability(victim);
        if (capability.soulData.spawnerSpawned || capability.soulData.soulless) return;

        var bounds = victim.getBoundingBox().inflate(8);
        var before = level.getEntitiesOfClass(com.sammy.malum.common.entity.spirit.SpiritItemEntity.class, bounds)
                .stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        if (SpiritHarvestHandler.getSpiritData(victim).isPresent()) {
            SpiritHarvestHandler.spawnSpirits(victim, recipient, ItemStack.EMPTY);
        } else if (victim instanceof Enemy || victim.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            int seed = id == null ? victim.getType().hashCode() : id.toString().hashCode();
            List<ItemStack> fallback = java.util.stream.IntStream.range(0,
                            EconomyPolicy.acquisition().unmappedHostileSpiritCount())
                    .mapToObj(offset -> spirit(SpiritKind.fromIndex(seed + offset * 3))).toList();
            LOGGER.error("Hostile entity {} has no Malum spirit mapping; using deterministic two-spirit fallback", id);
            SpiritHarvestHandler.spawnItemsAsSpirits(fallback, victim, recipient);
        }
        capability.soulData.soulless = true;
        for (var released : level.getEntitiesOfClass(com.sammy.malum.common.entity.spirit.SpiritItemEntity.class, bounds)) {
            if (before.contains(released.getUUID())) continue;
            var stack = released.getItem();
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && !stack.isEmpty()) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new com.bettercontent.economy.api.event.SpiritReleasedEvent(recipient, id, stack.getCount(), released.getUUID()));
        }
    }

    private static boolean isEconomyActor(final LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof IronGolem || entity.getType().is(SPIRITLESS_ACTORS);
    }

    private static ServerPlayer creditedPlayer(final Entity source, final LivingEntity killCredit) {
        ServerPlayer player = owner(source);
        return player != null ? player : owner(killCredit);
    }

    private static ServerPlayer owner(final Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    private static ItemStack spirit(final SpiritKind kind) {
        Item item = ForgeRegistries.ITEMS.getValue(kind.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
