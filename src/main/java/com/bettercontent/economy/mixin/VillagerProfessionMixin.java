package com.bettercontent.economy.mixin;

import com.bettercontent.economy.registry.SpiritProfessions;
import com.bettercontent.economy.spirit.SpiritKind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts every employed ordinary villager into one stable spirit profession. */
@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {
    @ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
    private VillagerData betterContentEconomy$onlySpiritProfessions(final VillagerData requested) {
        VillagerProfession profession = requested.getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT
                || SpiritProfessions.isSpiritProfession(profession)) return requested;
        Villager self = (Villager) (Object) this;
        SpiritKind kind = SpiritKind.fromIndex(self.getUUID().hashCode());
        return requested.setProfession(SpiritProfessions.definition(kind).profession().get());
    }

    @Inject(method = "getTypeName", at = @At("HEAD"), cancellable = true)
    private void betterContentEconomy$nameSpiritProfession(final CallbackInfoReturnable<Component> callback) {
        Villager self = (Villager) (Object) this;
        SpiritKind kind = SpiritProfessions.kindOf(self.getVillagerData().getProfession());
        if (kind != null) callback.setReturnValue(SpiritProfessions.displayName(kind));
    }
}
