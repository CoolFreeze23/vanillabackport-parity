package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.platform.common.worldgen.placement.BiomePlacement;
import com.blackgear.vanillabackport.common.integrations.worldgen.BiomeGeneration;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VB's appended sulfur-caves point is skipped: {@link SulfurOverworldBiomeBuilderMixin} adds it at its native place. */
@Mixin(BiomeGeneration.class)
public abstract class SulfurBiomeGenerationMixin {
    @WrapOperation(
        method = "bootstrap",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/platform/common/worldgen/placement/BiomePlacement$Event;add(Lcom/mojang/datafixers/util/Pair;)V")
    )
    private static void vbparity$sulfur$skipLateSulfurCaves(
        BiomePlacement.Event event, Pair<Climate.ParameterPoint, ResourceKey<Biome>> point, Operation<Void> original
    ) {
        if (point != BiomeGeneration.SULFUR_CAVES) {
            original.call(event, point);
        }
    }
}
