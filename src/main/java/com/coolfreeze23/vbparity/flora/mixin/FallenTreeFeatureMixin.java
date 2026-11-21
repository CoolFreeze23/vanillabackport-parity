package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.level.worldgen.features.FallenTreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** FLORA-06: native rejects the fallen log at the 3rd consecutive unsupported block ({@code ++gap > 2}); VB's {@code gaps++ > 2} at the 4th. */
@Mixin(FallenTreeFeature.class)
public abstract class FallenTreeFeatureMixin {
    @ModifyConstant(method = "canPlaceEntireFallenLog", constant = @Constant(intValue = 2))
    private int vbparity$nativeGapLimit(int limit) {
        return 1;
    }
}
