package com.bettercontent.economy.spirit;

import com.bettercontent.economy.BetterContentEconomy;
import com.bettercontent.economy.config.EconomyPolicy;
import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.ops.ObservationalEconomyData;
import com.bettercontent.economy.registry.CurrencyItems;
import com.mojang.logging.LogUtils;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.config.CommonConfig;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Sole authority for credited-kill currency. Native drops are evaluated before any release. */
public final class SpiritAcquisition {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DELIVERY_TAG = "better_content_economy_delivery";
    private static final String DELIVERY_STACK_TAG = "better_content_economy_delivery_stack";
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

        // Malum remains the source of the vector. The ordinary identities are replaced;
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
        ObservationalEconomyData.get(recipient.server.overworld()).recordRegion(regionKey(victim), credits);
        if (!credits.isEmpty()) ObservationalEconomyData.get(recipient.server.overworld()).recordActivity();
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
                List<PhysicalStack> stacks = currencyStacks(delivery.credits(), delivery.id());
                DeliveryReceipts receipts = findDeliveryReceipts(player, delivery.id());
                receipts.stackIndexes().addAll(data.inFlightStackReceipts(delivery.id()));
                // Turn every discoverable physical receipt into a durable claim before attempting
                // any missing stack. Loaded entity state is not a reliable retry witness after
                // its chunk unloads or an item is moved into offline custody.
                boolean observedReceiptAdded = false;
                for (int stackIndex : receipts.stackIndexes()) {
                    observedReceiptAdded |= data.recordStackReceipt(delivery.id(), stackIndex);
                }
                if (receipts.legacyReceipt()) {
                    // A pre-index receipt means the old implementation may have emitted any
                    // part or all of this manifest. Permanently consume every stack index.
                    for (PhysicalStack stack : stacks) {
                        int stackIndex = stack.grant().index();
                        observedReceiptAdded |= data.recordStackReceipt(delivery.id(), stackIndex);
                        receipts.stackIndexes().add(stackIndex);
                    }
                }
                if (observedReceiptAdded) {
                    data.setDirty();
                    overworld.getDataStorage().save();
                }
                if (receipts.legacyReceipt() || SpiritDeliveryPlan.isComplete(
                        stacks.stream().map(PhysicalStack::grant).toList(), receipts.stackIndexes())) {
                    ledger.acknowledge(delivery.id(), gameTime + cadence);
                    data.setDirty();
                    overworld.getDataStorage().save();
                    continue;
                }

                for (PhysicalStack stack : stacks) {
                    if (receipts.stackIndexes().contains(stack.grant().index())) continue;
                    int stackIndex = stack.grant().index();
                    // Claim and synchronously save this exact stack before the first operation
                    // that may expose it to the world. A crash from here onward may lose this
                    // stack, but cannot cause its automatic re-issuance.
                    if (!data.recordStackReceipt(delivery.id(), stackIndex)) {
                        receipts.stackIndexes().add(stackIndex);
                        continue;
                    }
                    data.setDirty();
                    overworld.getDataStorage().save();
                    receipts.stackIndexes().add(stackIndex);
                    if (!spawnCurrencyStack(player, stack.stack())) {
                        LOGGER.warn("Spirit delivery {} stack {} was not accepted by the level; its durable claim prevents retry",
                                delivery.id(), stackIndex);
                    }
                }
                if (!SpiritDeliveryPlan.isComplete(
                        stacks.stream().map(PhysicalStack::grant).toList(), receipts.stackIndexes())) {
                    ledger.failDelivery(delivery.id());
                    LOGGER.warn("Spirit delivery {} has unclaimed stacks; retaining its reservation for retry",
                            delivery.id());
                    continue;
                }
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

    @SubscribeEvent
    public static void onCurrencyPickup(final PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getStack();
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().hasUUID(DELIVERY_TAG)
                || !stack.getTag().contains(DELIVERY_STACK_TAG, net.minecraft.nbt.Tag.TAG_INT)) return;
        java.util.UUID deliveryId = stack.getTag().getUUID(DELIVERY_TAG);
        int stackIndex = stack.getTag().getInt(DELIVERY_STACK_TAG);
        var overworld = player.server.overworld();
        SpiritCreditData data = SpiritCreditData.get(overworld);
        if (data.recordStackReceipt(deliveryId, stackIndex)) {
            // A picked-up stack may be moved to an offline player or unloaded container and
            // consumed before retry can inspect its NBT. Persist the custody receipt immediately.
            data.setDirty();
            overworld.getDataStorage().save();
        }
    }

    private static List<PhysicalStack> currencyStacks(final Map<CurrencyIdentity, Integer> credits,
                                                      final java.util.UUID deliveryId) {
        List<SpiritDeliveryPlan.StackGrant> grants = SpiritDeliveryPlan.plan(credits,
                identity -> CurrencyItems.item(identity).get().getMaxStackSize());
        List<PhysicalStack> stacks = new ArrayList<>(grants.size());
        for (SpiritDeliveryPlan.StackGrant grant : grants) {
            Item item = CurrencyItems.item(grant.identity()).get();
            ItemStack stack = new ItemStack(item, grant.count());
            stack.getOrCreateTag().putUUID(DELIVERY_TAG, deliveryId);
            stack.getOrCreateTag().putInt(DELIVERY_STACK_TAG, grant.index());
            stacks.add(new PhysicalStack(grant, stack));
        }
        return List.copyOf(stacks);
    }

    private static DeliveryReceipts findDeliveryReceipts(final ServerPlayer player,
                                                         final java.util.UUID deliveryId) {
        // Per-stack witnesses let a retry skip accepted entities. Online inventories and loaded
        // item entities remain the discoverable custody boundary; offline/unloaded containers
        // are still not enumerable here.
        DeliveryReceipts receipts = new DeliveryReceipts();
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            online.getInventory().items.forEach(stack -> receipts.observe(stack, deliveryId));
            online.getInventory().offhand.forEach(stack -> receipts.observe(stack, deliveryId));
        }
        for (var level : player.server.getAllLevels()) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(),
                            level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(),
                            level.getWorldBorder().getMaxZ()))) {
                receipts.observe(item.getItem(), deliveryId);
            }
            for (SpiritItemEntity spirit : level.getEntitiesOfClass(SpiritItemEntity.class,
                    new net.minecraft.world.phys.AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(),
                            level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(),
                            level.getWorldBorder().getMaxZ()))) {
                receipts.observe(spirit.getItem(), deliveryId);
            }
        }
        return receipts;
    }

    private static boolean spawnCurrencyStack(final ServerPlayer player, final ItemStack stack) {
        Level level = player.level();
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() / 2.0;
        double z = player.getZ();
        double velocityX = level.random.nextFloat() * 0.2 - 0.1;
        double velocityY = level.random.nextFloat() * 0.25 + 0.25;
        double velocityZ = level.random.nextFloat() * 0.2 - 0.1;
        Entity entity;
        if (Boolean.TRUE.equals(CommonConfig.NO_FANCY_SPIRITS.getConfigValue())) {
            ItemEntity item = new ItemEntity(level, x, y, z, stack);
            item.setNoPickUpDelay();
            item.setDeltaMovement(velocityX, velocityY, velocityZ);
            entity = item;
        } else {
            entity = new SpiritItemEntity(level, player.getUUID(), stack,
                    x, y, z, velocityX, velocityY, velocityZ);
        }
        return level.addFreshEntity(entity);
    }

    private record PhysicalStack(SpiritDeliveryPlan.StackGrant grant, ItemStack stack) {}

    private static final class DeliveryReceipts {
        private final java.util.Set<Integer> stackIndexes = new java.util.HashSet<>();
        private boolean legacyReceipt;

        private void observe(final ItemStack stack, final java.util.UUID deliveryId) {
            if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().hasUUID(DELIVERY_TAG)
                    || !deliveryId.equals(stack.getTag().getUUID(DELIVERY_TAG))) return;
            if (stack.getTag().contains(DELIVERY_STACK_TAG, net.minecraft.nbt.Tag.TAG_INT)) {
                stackIndexes.add(stack.getTag().getInt(DELIVERY_STACK_TAG));
            } else {
                // Migrate in-flight receipts created before per-stack indices existed.
                legacyReceipt = true;
            }
        }

        private java.util.Set<Integer> stackIndexes() { return stackIndexes; }
        private boolean legacyReceipt() { return legacyReceipt; }
    }

    private static boolean isEconomyActor(final LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof IronGolem || entity.getType().is(SPIRITLESS_ACTORS);
    }

    private static ServerPlayer creditedPlayer(final Entity source) { return playerSource(source); }

    private static long regionSeed(final LivingEntity victim) {
        return regionIdentity(victim).seed();
    }
    private static String regionKey(final LivingEntity victim) {
        return regionIdentity(victim).key();
    }
    private static SpiritRegionIdentity regionIdentity(final LivingEntity victim) {
        var level = victim.level();
        var biome = level.getBiome(victim.blockPosition()).unwrapKey();
        return SpiritRegionIdentity.of(level.dimension().location().toString(),
                biome.map(key -> key.location().toString()).orElse("unknown"));
    }

    // Accept direct, projectile, and spell-projectile kills while excluding OwnableEntity mobs.
    private static ServerPlayer playerSource(final Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) return player;
        // Deliberately mapped passive effects whose owner is explicit in vanilla.  Tamable or
        // arbitrary owned mobs are not followed, preventing unattended farming attribution.
        if (entity instanceof AreaEffectCloud cloud && cloud.getOwner() instanceof ServerPlayer player) return player;
        if (entity instanceof EvokerFangs fangs && fangs.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    private static ItemStack spirit(final SpiritKind kind) {
        Item item = ForgeRegistries.ITEMS.getValue(kind.itemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
