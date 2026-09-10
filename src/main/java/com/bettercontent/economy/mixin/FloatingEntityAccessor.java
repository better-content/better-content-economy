package com.bettercontent.economy.mixin;

import com.sammy.malum.common.entity.FloatingEntity;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FloatingEntity.class, remap = false)
public interface FloatingEntityAccessor {
    @Accessor("owner") LivingEntity betterContentEconomy$getOwner();
    @Accessor("ownerUUID") UUID betterContentEconomy$getOwnerUuid();
}
