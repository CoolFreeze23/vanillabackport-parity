package com.coolfreeze23.vbparity.variants.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** VARIANTS-11: native {@code WolfArmorLayer} never draws armor on puppies ({@code && !state.isBaby}). */
@Mixin(WolfArmorLayer.class)
public abstract class WolfArmorLayerMixin {
    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Wolf;FFFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void vbp$noArmorOnPuppies(PoseStack poseStack, MultiBufferSource buffer, int light, Wolf wolf, float limbSwing, float limbSwingAmount,
                                      float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (wolf.isBaby()) {
            ci.cancel();
        }
    }
}
