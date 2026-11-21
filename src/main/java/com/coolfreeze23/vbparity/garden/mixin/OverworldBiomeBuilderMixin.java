package com.coolfreeze23.vbparity.garden.mixin;

import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.garden.GardenWorldgen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native 26.2 OverworldBiomeBuilder: PLATEAU_BIOMES[2][4] (neutral temperature, humid) is PALE_GARDEN instead of
 * DARK_FOREST; PLATEAU_BIOMES_VARIANT[2][4] is null in both versions. Gated by VB's pale_garden config,
 * read at common setup (see GardenWorldgen).
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @Inject(method = "pickPlateauBiome", at = @At("RETURN"), cancellable = true)
    private void vbparity$garden$paleGardenPlateau(int temperature, int humidity, Climate.Parameter weirdness, CallbackInfoReturnable<ResourceKey<Biome>> cir) {
        if (temperature == 2 && humidity == 4 && cir.getReturnValue() == Biomes.DARK_FOREST && GardenWorldgen.paleGardenPlateau()) {
            cir.setReturnValue(ModBiomes.PALE_GARDEN);
        }
    }
}
