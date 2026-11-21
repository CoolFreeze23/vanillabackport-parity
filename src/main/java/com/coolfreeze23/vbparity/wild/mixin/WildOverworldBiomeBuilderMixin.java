package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.wild.WildConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WILD-01. Native 26.3 OverworldBiomeBuilder: MIDDLE_BIOMES_VARIANT[1][0] (cool, arid) is DAPPLED_FOREST instead of null,
 * so every positive-weirdness PLAINS cell the middle-biome picker produces there becomes dappled forest, at both surface
 * and floor depth. pickMiddleBiome is the only reader of that array. Gated by VB's has_dappled_forest (read at common setup).
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class WildOverworldBiomeBuilderMixin {
    @Inject(method = "pickMiddleBiome", at = @At("RETURN"), cancellable = true)
    private void vbparity$wild$dappledForestVariant(int temperature, int humidity, Climate.Parameter weirdness, CallbackInfoReturnable<ResourceKey<Biome>> cir) {
        if (temperature == 1 && humidity == 0 && weirdness.max() >= 0L && WildConfig.dappledForestPlacement()) {
            cir.setReturnValue(ModBiomes.DAPPLED_FOREST);
        }
    }
}
