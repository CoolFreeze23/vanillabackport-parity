package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.worldgen.surface.ModSurfaceRuleData;
import com.coolfreeze23.vbparity.sulfur.SulfurSurfaceRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SULFUR-44. Native large_biomes, amplified and caves noise settings use the same overworld-like rule with the sulfur bands;
 * VB only injects its rules for minecraft:overworld. Same merge as VB's own injection, for the other presets. Left alone
 * with TerraBlender, which adds the rules through its own surface-rule manager.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @WrapOperation(
        method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/blending/Blender;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;surfaceRule()Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;")
    )
    private SurfaceRules.RuleSource vbp$sulfurRulesForOverworldPresets(NoiseGeneratorSettings instance, Operation<SurfaceRules.RuleSource> original) {
        SurfaceRules.RuleSource rule = original.call(instance);
        if (!SulfurSurfaceRules.terraBlenderLoaded()
            && (this.settings.is(NoiseGeneratorSettings.LARGE_BIOMES) || this.settings.is(NoiseGeneratorSettings.AMPLIFIED) || this.settings.is(NoiseGeneratorSettings.CAVES))) {
            return ModSurfaceRuleData.merge(rule, List.of(ModSurfaceRuleData.makeRules()));
        }
        return rule;
    }
}
