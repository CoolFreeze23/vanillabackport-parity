package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.model.entity.CopperGolemModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * COPPER-25: VB's block-decoration transform ends 0.05 block off native (-2.25 + 1.45 against native -1.75 + 1.0).
 * -2.2 puts the antenna block at the native height.
 */
@Mixin(CopperGolemModel.class)
public abstract class CopperGolemModelMixin {
    @ModifyConstant(method = "applyBlockOnAntennaTransform", constant = @Constant(doubleValue = -2.25))
    private double vbparity$antennaOffset(double original) {
        return -2.2;
    }
}
