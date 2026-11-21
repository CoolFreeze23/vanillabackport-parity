package com.coolfreeze23.vbparity.copper.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native IronBarsBlock.skipRendering also culls the touching faces of two different #bars blocks (iron bars next to
 * copper bars, or two copper oxidation stages), not only of the same block.
 */
@Mixin(IronBarsBlock.class)
public abstract class IronBarsBlockMixin {
    @Unique
    private static final TagKey<Block> VBPARITY$BARS = TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("bars"));

    @ModifyExpressionValue(
        method = "skipRendering",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean vbparity$copper$barsCullEachOther(
        boolean sameBlock, @Local(argsOnly = true, ordinal = 0) BlockState state, @Local(argsOnly = true, ordinal = 1) BlockState neighbour,
        @Local(argsOnly = true) Direction side
    ) {
        return sameBlock
            || neighbour.is(VBPARITY$BARS) && state.is(VBPARITY$BARS) && vbparity$hasSide(neighbour, side.getOpposite());
    }

    @Unique
    private static boolean vbparity$hasSide(BlockState state, Direction direction) {
        return switch (direction) {
            case NORTH -> state.hasProperty(IronBarsBlock.NORTH);
            case SOUTH -> state.hasProperty(IronBarsBlock.SOUTH);
            case EAST -> state.hasProperty(IronBarsBlock.EAST);
            case WEST -> state.hasProperty(IronBarsBlock.WEST);
            default -> false;
        };
    }
}
