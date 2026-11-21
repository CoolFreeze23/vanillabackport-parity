package com.coolfreeze23.vbparity.ghast.mixin.client;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** GHAST-39: the return strip of the lead ribbon uses fudge 0, giving the current game's crossed (X-shaped) lead. */
@Mixin(LeashFeatureRenderer.class)
public abstract class LeashFeatureRendererMixin {
    @ModifyArg(method = "renderLeash", at = @At(value = "INVOKE",
        target = "Lcom/blackgear/vanillabackport/common/api/modules/leash_behavior/LeashFeatureRenderer;addVertexPair(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;FFFFFFIZLcom/blackgear/vanillabackport/common/api/modules/leash_behavior/LeashState;)V",
        ordinal = 1), index = 5)
    private float vbp$backStripFudge(float fudge) {
        return 0.0F;
    }
}
