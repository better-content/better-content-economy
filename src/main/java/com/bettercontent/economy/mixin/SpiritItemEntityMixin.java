package com.bettercontent.economy.mixin;

import com.bettercontent.economy.api.event.SpiritAcquiredEvent;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpiritItemEntity.class, remap = false)
abstract class SpiritItemEntityMixin {
    @Inject(method = "collect", at = @At("TAIL"), remap = false)
    private void betterContentEconomy$signalCollection(final CallbackInfo callback) {
        SpiritItemEntity self = (SpiritItemEntity) (Object) this;
        if (!(((FloatingEntityAccessor) this).betterContentEconomy$getOwner() instanceof ServerPlayer player)) return;
        ItemStack stack = self.getItem();
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && "malum".equals(id.getNamespace()) && id.getPath().endsWith("_spirit")) {
            MinecraftForge.EVENT_BUS.post(new SpiritAcquiredEvent(player, id, stack.getCount(), self.getUUID()));
        }
    }
}
