package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.integrations.worldgen.BiomeGeneration;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Pale garden placement. VB appends its own pale garden climate points (mid/high slice variants and peak variants,
 * with Platform's FAR_INLAND = 0.03..1.0) after the vanilla list. Native 26.2 instead replaces DARK_FOREST in
 * OverworldBiomeBuilder.PLATEAU_BIOMES[2][4] with PALE_GARDEN; OverworldBiomeBuilderMixin does that, so VB's extra
 * points are dropped here (this also empties the list VB's TerraBlender region reads).
 */
@Mixin(BiomeGeneration.class)
public abstract class BiomeGenerationMixin {
    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;make(Ljava/util/function/Supplier;)Ljava/lang/Object;", ordinal = 0)
    )
    private static Object vbparity$garden$noExtraPaleGardenPoints(Object paleGardenPoints) {
        return List.of();
    }
}
