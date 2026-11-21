package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SULFUR-38. Native sulfur_pool has can_place_feature = not(sulfur_spike): a shell position holding a sulfur spike drops
 * the whole pool. The first getBlockState in place() is the shell check; reporting such a spike as lava makes both
 * existing shell tests fail, which is exactly native's "return false". Only lakes with a sulfur barrier (sulfur_pool).
 */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
    @WrapOperation(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0
        )
    )
    private BlockState vbp$sulfurPoolAvoidsSpikes(
        WorldGenLevel level, BlockPos pos, Operation<BlockState> original, @Local(argsOnly = true) FeaturePlaceContext<LakeFeature.Configuration> context
    ) {
        BlockState state = original.call(level, pos);
        if (state.is(ModBlocks.SULFUR_SPIKE.get())
            && context.config().barrier() instanceof SimpleStateProvider barrier
            && barrier.getState(context.random(), pos).is(ModBlocks.SULFUR.get())) {
            return Blocks.LAVA.defaultBlockState();
        }
        return state;
    }
}
