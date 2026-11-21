package com.coolfreeze23.vbparity.flora.worldgen;

import java.util.List;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.FeatureSorter;

/** Implemented by ChunkGenerator (mixin): the natively re-indexed feature order it decorates with. */
public interface NativeStepsAccess {
    List<FeatureSorter.StepFeatureData> vbparity$nativeSteps(WorldGenLevel level, boolean wildernessBound);

    List<FeatureSorter.StepFeatureData> vbparity$runtimeSteps();
}
