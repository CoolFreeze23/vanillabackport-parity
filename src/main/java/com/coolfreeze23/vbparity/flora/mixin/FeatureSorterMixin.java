package com.coolfreeze23.vbparity.flora.mixin;

import com.coolfreeze23.vbparity.flora.worldgen.VegetalOrderModifier;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Safety net for FLORA-12: if moving VB's flora features to their native positions makes the global feature order
 * cyclic (another mod lists the same features the other way round), sort with VB's original lists instead of crashing.
 */
@Mixin(FeatureSorter.class)
public abstract class FeatureSorterMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapMethod(method = "buildFeaturesPerStep")
    private static List<FeatureSorter.StepFeatureData> vbparity$fallBackToVbOrder(
        List featureSetSources, Function toFeatureSetFunction, boolean outermost, Operation<List<FeatureSorter.StepFeatureData>> original
    ) {
        try {
            return original.call(featureSetSources, toFeatureSetFunction, outermost);
        } catch (IllegalStateException e) {
            if (!outermost || VegetalOrderModifier.ORIGINAL.isEmpty()) {
                throw e;
            }
            VegetalOrderModifier.logFallback(e);
            Function<Object, List<HolderSet<PlacedFeature>>> base = toFeatureSetFunction;
            Function<Object, List<HolderSet<PlacedFeature>>> vbOrder = source -> {
                List<HolderSet<PlacedFeature>> steps = base.apply(source);
                if (source instanceof Holder<?> holder && holder.unwrapKey().isPresent()) {
                    List<Holder<PlacedFeature>> before = VegetalOrderModifier.ORIGINAL.get((ResourceKey<Biome>) holder.unwrapKey().get());
                    int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
                    if (before != null && steps.size() > step) {
                        steps = new ArrayList<>(steps);
                        steps.set(step, HolderSet.direct(before));
                    }
                }
                return steps;
            };
            return original.call(featureSetSources, vbOrder, outermost);
        }
    }
}
