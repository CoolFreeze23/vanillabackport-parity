package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.integrations.worldgen.BiomeGeneration;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * WILD-01. VB appends its own dappled forest points (mid/high slice E2-E4, surface depth only) after vanilla's PLAINS
 * points for the same cells, so the two tie. Native places it through OverworldBiomeBuilder instead (see
 * WildOverworldBiomeBuilderMixin), so VB's list is emptied; this also empties the list VB's TerraBlender region reads.
 * Util.make ordinal 0 is PALE_GARDEN (garden area), ordinal 1 is DAPPLED_FOREST.
 */
@Mixin(BiomeGeneration.class)
public abstract class WildBiomeGenerationMixin {
    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;make(Ljava/util/function/Supplier;)Ljava/lang/Object;", ordinal = 1)
    )
    private static Object vbparity$wild$noExtraDappledForestPoints(Object dappledForestPoints) {
        return List.of();
    }
}
