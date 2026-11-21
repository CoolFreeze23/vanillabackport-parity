package com.coolfreeze23.vbparity.variants.mixin.client;

import com.blackgear.vanillabackport.client.level.layer.SheepWoolUndercoatLayer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VARIANTS-15: native draws the undercoat only on adult sheep that are coloured or named jeb_
 * ({@code (isJebSheep || woolColor != WHITE) && !isBaby}). VARIANTS-16: the jeb_ cycle has no entity-id offset.
 */
@Mixin(SheepWoolUndercoatLayer.class)
public abstract class SheepWoolUndercoatLayerMixin {
    @Unique
    private static final String RENDER =
        "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Sheep;FFFFFF)V";

    @Inject(method = RENDER, at = @At("HEAD"), cancellable = true)
    private void vbp$nativeConditions(PoseStack poseStack, MultiBufferSource buffer, int light, Sheep sheep, float limbSwing, float limbSwingAmount,
                                      float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        boolean jeb = sheep.hasCustomName() && "jeb_".equals(sheep.getName().getString());
        if (sheep.isBaby() || !jeb && sheep.getColor() == DyeColor.WHITE) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;getId()I"))
    private int vbp$noIdOffset(int original) {
        return 0;
    }
}
