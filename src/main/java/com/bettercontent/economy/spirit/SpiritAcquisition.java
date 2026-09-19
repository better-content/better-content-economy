package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.config.EconomyConfig;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Sole authority for credited-kill currency. Native drops are evaluated before any release. */
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
        ServerPlayer recipient = creditedPlayer(event.getSource().getEntity());
        if (recipient == null) return;

        var capability = MalumLivingEntityDataCapability.getCapability(victim);
        if (capability.soulData.spawnerSpawned || capability.soulData.soulless) return;

        // Malum remains the source of the vector. The seven ordinary identities are replaced;
        // exotic drops stay native Malum items.
        List<ItemStack> nativeDrops = SpiritHarvestHandler.getSpawnedSpirits(victim, recipient, ItemStack.EMPTY);
        if (nativeDrops.isEmpty() && (victim instanceof Enemy || victim.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            int seed = id == null ? victim.getType().hashCode() : id.toString().hashCode();
            nativeDrops = java.util.stream.IntStream.range(0, EconomyPolicy.acquisition().unmappedHostileSpiritCount())
                    .mapToObj(offset -> spirit(SpiritKind.fromIndex(seed + offset * 3))).filter(stack -> !stack.isEmpty()).toList();
            LOGGER.error("Hostile entity {} has no Malum spirit mapping; using deterministic two-spirit fallback", id);
        }

        Map<CurrencyIdentity, Integer> credits = SpiritCreditAllocation.fromNative(nativeDrops, victim.getUUID(),
                recipient.getUUID(), regionSeed(victim));
        List<ItemStack> exotic = nativeDrops.stream().filter(stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null || CurrencyIdentity.fromLegacyNativeSpirit(id) == null;
        }).toList();
        if (!exotic.isEmpty()) SpiritHarvestHandler.spawnItemsAsSpirits(exotic, victim, recipient);
        if (!credits.isEmpty()) {
            SpiritCreditData data = SpiritCreditData.get(recipient.server.overworld());
            data.ledger(recipient.getUUID()).credit(credits, recipient.server.overworld().getGameTime() + EconomyConfig.spiritReleaseCadenceTicks());
            data.setDirty();
        }
        capability.soulData.soulless = true;
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        int cadence = EconomyConfig.spiritReleaseCadenceTicks();
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % cadence != 0) return;
        var overworld = event.getServer().overworld();
        SpiritCreditData data = SpiritCreditData.get(overworld);
        long gameTime = overworld.getGameTime();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SpiritCreditLedger ledger = data.ledger(player.getUUID());
            SpiritCreditLedger.Delivery delivery = ledger.retryDelivery();
            if (delivery == null) {
                delivery = ledger.beginDueDelivery(gameTime);
                // Persist the reservation before any physical stacks are created. A later retry
                // has the identical receipt rather than creating a second debit.
                if (delivery != null) {
                    data.setDirty();
                    overworld.getDataStorage().save();
                }
            }
            if (delivery == null) continue;
            try {
                // A restart between entity insertion and acknowledgement leaves the same receipt
                // on the owned physical stack. Recognize it before any retry can create another.
                if (hasDeliveryReceipt(player, delivery.id())) {
                    ledger.acknowledge(delivery.id(), gameTime + cadence);
                    data.setDirty();
                    overworld.getDataStorage().save();
                    continue;
                }
                List<ItemStack> stacks = currencyStacks(delivery.credits(), delivery.id());
                if (!stacks.isEmpty()) SpiritHarvestHandler.spawnItemsAsSpirits(stacks, player, player);
                ledger.acknowledge(delivery.id(), gameTime + cadence);
                data.setDirty();
                overworld.getDataStorage().save();
            } catch (RuntimeException exception) {
                ledger.failDelivery(delivery.id());
                data.setDirty();
                LOGGER.warn("Will retry spirit credit delivery {} for {}", delivery.id(), player.getGameProfile().getName(), exception);
            }
        }
    }

    private static List<ItemStack> currencyStacks(final Map<CurrencyIdentity, Integer> credits, final java.util.UUID deliveryId) {
        List<ItemStack> stacks = new ArrayList<>();
        credits.forEach((identity, total) -> {
            Item item = CurrencyItems.item(identity).get();
            for (int count = total; count > 0; count -= item.getMaxStackSize()) {
                ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
                stack.getOrCreateTag().putUUID("better_content_economy_delivery", deliveryId);
                stacks.add(stack);
            }
        });
        return stacks;
    }

    private static boolean hasDeliveryReceipt(final ServerPlayer player, final java.util.UUID deliveryId) {
        // Ownership can transfer before an interruption, including when the recipient changes
        // dimension. Loaded entities and every online inventory are therefore authoritative
        // receipt witnesses. Offline or unloaded third-party custody remains intentionally
        // unresolved rather than being guessed and duplicated.
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (online.getInventory().items.stream().anyMatch(stack -> deliveryId.equals(receipt(stack)))) return true;
            if (online.getInventory().offhand.stream().anyMatch(stack -> deliveryId.equals(receipt(stack)))) return true;
        }
        for (var level : player.server.getAllLevels()) {
            if (level.getEntitiesOfClass(com.sammy.malum.common.entity.spirit.SpiritItemEntity.class,
                    new net.minecraft.world.phys.AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(),
                            level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(),
                            level.getWorldBorder().getMaxZ())).stream()
                    .map(com.sammy.malum.common.entity.spirit.SpiritItemEntity::getItem)
                    .anyMatch(stack -> deliveryId.equals(receipt(stack)))) return true;
        }
        return false;
    }

    private static java.util.UUID receipt(final ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("better_content_economy_delivery")
                ? stack.getTag().getUUID("better_content_economy_delivery") : null;
    }

    private static boolean isEconomyActor(final LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof IronGolem || entity.getType().is(SPIRITLESS_ACTORS);
    }

    private static ServerPlayer creditedPlayer(final Entity source) { return playerSource(source); }

    private static long regionSeed(final LivingEntity victim) {
        var pos = victim.blockPosition();
        long seed = victim.level().dimension().location().hashCode();
        seed = seed * 31L + pos.getX() / 16;
        seed = seed * 31L + pos.getZ() / 16;
        var biome = victim.level().getBiome(pos).unwrapKey();
        return seed * 31L + (biome.isPresent() ? biome.get().location().hashCode() : 0L);
    }

    // Accept direct, projectile, and spell-projectile kills while excluding OwnableEntity mobs.
    private static ServerPlayer playerSource(final Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    private static ItemStack spirit(final SpiritKind kind) {
        Item item = ForgeRegistries.ITEMS.getValue(kind.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
