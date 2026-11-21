package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.flora.worldgen.NativeFeatureOrder;
import com.coolfreeze23.vbparity.flora.worldgen.NativeStepsAccess;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * FLORA-12: overworld features are placed in the native global order and seeded with their native index
 * (see {@link NativeFeatureOrder}). A chunk whose 3x3 neighbourhood holds a Wilderness Bound biome uses the 26.3 order
 * (the biome only exists there); every other chunk uses the 26.2 order.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements NativeStepsAccess {
    @Shadow
    @Final
    protected BiomeSource biomeSource;

    @Shadow
    @Final
    private Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;

    @Unique
    private volatile NativeFeatureOrder.Remapped vbparity$native;

    @ModifyVariable(
        method = "applyBiomeDecoration",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;retainAll(Ljava/util/Collection;)Z", shift = At.Shift.AFTER),
        ordinal = 0
    )
    private List<FeatureSorter.StepFeatureData> vbparity$flora$nativeFeatureOrder(
        List<FeatureSorter.StepFeatureData> steps, WorldGenLevel level, @Local Set<Holder<Biome>> biomes
    ) {
        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return steps;
        }
        NativeFeatureOrder.Remapped remapped = this.vbparity$nativeFor(steps, level);
        boolean wildernessBound = false;
        for (Holder<Biome> biome : biomes) {
            if (biome.is(ModBiomes.DAPPLED_FOREST)) {
                wildernessBound = true;
                break;
            }
        }
        return wildernessBound ? remapped.v263() : remapped.v262();
    }

    @Unique
    private NativeFeatureOrder.Remapped vbparity$nativeFor(List<FeatureSorter.StepFeatureData> steps, WorldGenLevel level) {
        NativeFeatureOrder.Remapped remapped = this.vbparity$native;
        if (remapped == null || remapped.source() != steps) {
            synchronized (this) {
                remapped = this.vbparity$native;
                if (remapped == null || remapped.source() != steps) {
                    remapped = NativeFeatureOrder.Remapped.of(
                        steps, this.biomeSource.possibleBiomes(), level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                    );
                    this.vbparity$native = remapped;
                }
            }
        }
        return remapped;
    }

    @Override
    public List<FeatureSorter.StepFeatureData> vbparity$nativeSteps(WorldGenLevel level, boolean wildernessBound) {
        NativeFeatureOrder.Remapped remapped = this.vbparity$nativeFor(this.featuresPerStep.get(), level);
        return wildernessBound ? remapped.v263() : remapped.v262();
    }

    @Override
    public List<FeatureSorter.StepFeatureData> vbparity$runtimeSteps() {
        return this.featuresPerStep.get();
    }
}
