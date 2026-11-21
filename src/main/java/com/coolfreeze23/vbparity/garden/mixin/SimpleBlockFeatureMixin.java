package com.coolfreeze23.vbparity.garden.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import org.spongepowered.asm.mixin.Mixin;

/**
 * GARDEN-19: native SimpleBlockFeature.place samples the state provider once. VB's HEAD injector samples it, and for
 * anything that is not a mossy carpet or an eyeblossom the 1.21.1 body samples it again. This wrapper draws the one
 * native sample itself and hands the original method (VB's injector, the 1.21.1 body and any other mod's injectors) a
 * fixed SimpleStateProvider of that state, which draws no random values. VB's injector then gives native results:
 * MossyCarpetBlock.placeAt for carpets, and the 1-tick scheduled tick for eyeblossoms (native schedule_tick: true on
 * flower_pale_garden / pale_forest_flower, the only features that place them). Fixed providers already sample nothing,
 * so they go straight to the original.
 */
@Mixin(SimpleBlockFeature.class)
public abstract class SimpleBlockFeatureMixin {
    @WrapMethod(method = "place")
    private boolean vbparity$garden$sampleOnce(FeaturePlaceContext<SimpleBlockConfiguration> context, Operation<Boolean> original) {
        BlockStateProvider provider = context.config().toPlace();
        if (provider instanceof SimpleStateProvider) {
            return original.call(context);
        }
        BlockState state = provider.getState(context.random(), context.origin());
        FeaturePlaceContext<SimpleBlockConfiguration> fixed = new FeaturePlaceContext<>(
            context.topFeature(), context.level(), context.chunkGenerator(), context.random(), context.origin(),
            new SimpleBlockConfiguration(BlockStateProvider.simple(state))
        );
        return original.call(fixed);
    }
}
