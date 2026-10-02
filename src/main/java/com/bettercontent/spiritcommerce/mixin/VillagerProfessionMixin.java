package com.bettercontent.spiritcommerce.mixin;

import com.bettercontent.spiritcommerce.registry.SpiritProfessions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve names on existing spirit profession villagers without converting new villagers. */
@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {
    @Inject(method = "getTypeName", at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$nameSpiritProfession(final CallbackInfoReturnable<Component> callback) {
        Villager self = (Villager) (Object) this;
        com.bettercontent.spiritcommerce.spirit.SpiritKind kind = SpiritProfessions.kindOf(self.getVillagerData().getProfession());
        if (kind != null) callback.setReturnValue(SpiritProfessions.displayName(kind));
    }
}
