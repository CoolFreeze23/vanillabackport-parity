package com.coolfreeze23.vbparity.ghast.mixin.client;

import com.blackgear.vanillabackport.client.level.model.entity.happy_ghast.HappyGhastModel;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GHAST-20: the shared model instance resets the body scale every frame (the current game resets every part's pose),
 * so the harnessed 0.9375 squeeze does not stick to ghasts drawn after a harnessed one.
 */
@Mixin(HappyGhastModel.class)
public abstract class HappyGhastModelMixin {
    @Shadow
    @Final
    private ModelPart body;

    @Inject(method = "setupAnim(Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/happy_ghast/HappyGhast;FFFFF)V", at = @At("HEAD"))
    private void vbp$resetBodyScale(HappyGhast entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        this.body.xScale = 1.0F;
        this.body.yScale = 1.0F;
        this.body.zScale = 1.0F;
    }
}
