package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.blocks.ShelfBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * COPPER-13: native shelves only give a comparator signal out of their back. 1.21.1 getAnalogOutputSignal has no
 * direction, so the comparator applies the native test: the shelf faces the same way as the comparator reading it.
 */
@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin {
    @WrapOperation(
        method = "getInputSignal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getAnalogOutputSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"
        )
    )
    private int vbparity$shelfBackOnly(
        BlockState target, Level level, BlockPos targetPos, Operation<Integer> original, @Local(argsOnly = true) BlockState comparatorState
    ) {
        if (target.getBlock() instanceof ShelfBlock
            && comparatorState.getValue(ComparatorBlock.FACING) != target.getValue(ShelfBlock.FACING)) {
            return 0;
        }
        return original.call(target, level, targetPos);
    }
}
