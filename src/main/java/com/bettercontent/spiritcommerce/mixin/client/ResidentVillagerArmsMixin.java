package com.bettercontent.spiritcommerce.mixin.client;

import com.bettercontent.spiritcommerce.resident.ResidentRules;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Village residents have separate arms that can show their gathering swing. */
@Mixin(VillagerModel.class)
abstract class ResidentVillagerArmsMixin<T extends Entity> {
    @Shadow @Final private ModelPart root;

    @Inject(method = "createBodyModel", at = @At("RETURN"))
    private static void betterContentEconomy$addIndependentArms(CallbackInfoReturnable<MeshDefinition> callback) {
        var body = callback.getReturnValue().getRoot();
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 22)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-6, 2, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 22).mirror()
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(6, 2, 0));
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void betterContentEconomy$poseIndependentArms(T entity, float limbSwing, float limbSwingAmount,
                                                            float ageInTicks, float headYaw, float headPitch,
                                                            CallbackInfo callback) {
        ModelPart right = root.getChild("right_arm");
        ModelPart left = root.getChild("left_arm");
        boolean resident = entity instanceof Villager && ResidentRules.isResident(entity);
        root.getChild("arms").visible = !resident;
        right.visible = resident;
        left.visible = resident;
        if (!resident) return;
        right.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        left.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float swing = ((LivingEntity) entity).getAttackAnim(ageInTicks);
        right.xRot -= Mth.sin(Mth.sqrt(swing) * Mth.PI) * 1.4F;
    }
}
