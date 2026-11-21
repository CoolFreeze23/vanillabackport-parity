package com.coolfreeze23.vbparity.wild.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.object.CushionRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * WILD-33. Native CushionRenderer: rotateDegrees(Axis.YP, 180 - direction.toYRot()). VB rotates by toYRot(), which
 * agrees for east/west but turns north/south cushions around. Ordinal 0 is the Y rotation (ordinal 1 is the X flip).
 */
@Mixin(CushionRenderer.class)
public abstract class CushionRendererMixin {
    @ModifyArg(
        method = "render(Lcom/blackgear/vanillabackport/common/level/entities/decoration/Cushion;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0)
    )
    private float vbparity$wild$nativeFacing(float degrees) {
        return 180.0F - degrees;
    }
}
