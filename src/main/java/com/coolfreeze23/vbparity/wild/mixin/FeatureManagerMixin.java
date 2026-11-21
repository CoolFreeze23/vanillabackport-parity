package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.platform.common.worldgen.modifier.BiomeContext;
import com.blackgear.platform.common.worldgen.modifier.FeatureManager;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.blackgear.vanillabackport.common.worldgen.SpringToLifeFeatureManager;
import com.blackgear.vanillabackport.common.worldgen.placements.SpringToLifePlacements;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WILD-05 / WILD-04. Native 26.3 dappled forest vegetation is glow_lichen, trees_dappled_forest,
 * brown_mushroom_dappled_forest, patch_red_shrub, patch_grass_forest. VB's Spring to Life manager also gives it (through
 * #is_forest and #spawns_leaf_litter) firefly bushes near water, rare fallen birches and the chunk-scan leaf litter.
 * Native litter there comes from the poplar *_leaf_litter tree decorators (data override), so all three are skipped for
 * the dappled forest, whatever the tags say. Platform's FeatureManager is VB's base class; only
 * SpringToLifeFeatureManager is affected.
 */
@Mixin(FeatureManager.class)
public abstract class FeatureManagerMixin {
    @Shadow @Final private BiomeContext context;

    @Unique
    private static final Set<ResourceKey<PlacedFeature>> VBPARITY$NOT_IN_DAPPLED_FOREST = Set.of(
        SpringToLifePlacements.PATCH_FIREFLY_BUSH_NEAR_WATER,
        SpringToLifePlacements.PLACED_RARE_FALLEN_BIRCH_TREE,
        SpringToLifePlacements.LEAF_LITTER_THICK,
        SpringToLifePlacements.LEAF_LITTER_SPARSE
    );

    @Inject(method = "addVegetation", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$nativeDappledForestVegetation(ResourceKey<PlacedFeature> feature, CallbackInfo ci) {
        if ((Object) this instanceof SpringToLifeFeatureManager
            && VBPARITY$NOT_IN_DAPPLED_FOREST.contains(feature)
            && this.context.is(ModBiomes.DAPPLED_FOREST)) {
            ci.cancel();
        }
    }
}
