package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.item.CopperGolemStatueItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * A dropped statue: native ItemEntityRenderer rests the model's ground-transformed bounding box (y -0.35..0.175 for the
 * statue) at 0.0625 above the item position, so it spans 0.0625..0.5875. 1.21.1 lifts every item by
 * 0.25 * ground scale (0.0875) and VB's GROUND translate (0, 0.5, 1) leaves the statue spanning 0.0875..0.6125, 0.025
 * block high. In item space the ground transform maps +y to -0.35 y, so the translate becomes 0.5 + 0.025 / 0.35.
 * Every other context already matches native's T(0.5, 1.5, 0.5) S(1, -1, -1) item transformation.
 */
@Mixin(CopperGolemStatueItemRenderer.class)
public abstract class CopperGolemStatueItemRendererMixin {
    @ModifyConstant(method = "render", constant = @Constant(floatValue = 0.5F))
    private float vbparity$groundHeight(float original) {
        return 0.5F + 0.025F / 0.35F;
    }
}
