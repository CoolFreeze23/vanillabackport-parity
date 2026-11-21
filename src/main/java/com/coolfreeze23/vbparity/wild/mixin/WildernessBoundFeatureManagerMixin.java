package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.worldgen.WildernessBoundFeatureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WILD-03. VB adds fallen poplars as their own 1-in-80 vegetal feature. Native has them as the weight-120 entry of
 * trees_dappled_forest (data override, switched on has_fallen_trees), so VB's extra feature is not added.
 */
@Mixin(WildernessBoundFeatureManager.class)
public abstract class WildernessBoundFeatureManagerMixin {
    @Inject(method = "bootstrap", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$noStandaloneFallenPoplars(CallbackInfo ci) {
        ci.cancel();
    }
}
