package com.coolfreeze23.vbparity.wild.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.item.StrawBedItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * WILD-23. Native items/straw_bed.json is a composite of the head and foot block models, each turned 180 degrees about Y
 * and moved by [1,0,1] / [1,0,2], under the block models' display. VB's item display keeps an extra 180 degrees in its
 * rotations instead; with that, the native layout is the head at z offset 0 and the foot at -1 (VB: -0.3 / -1.3).
 * Ordinal 0 is the -0.5 centering, 1 the head, 2 the foot.
 */
@Mixin(StrawBedItemRenderer.class)
public abstract class StrawBedItemRendererMixin {
    @ModifyArg(
        method = "renderFirstPerson",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1),
        index = 2
    )
    private float vbparity$wild$headOffset(float z) {
        return 0.0F;
    }

    @ModifyArg(
        method = "renderFirstPerson",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 2),
        index = 2
    )
    private float vbparity$wild$footOffset(float z) {
        return -1.0F;
    }
}
