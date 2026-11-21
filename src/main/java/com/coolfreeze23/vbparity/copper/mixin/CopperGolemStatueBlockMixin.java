package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.blocks.CopperGolemStatueBlock;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemStatueBlock.class)
public abstract class CopperGolemStatueBlockMixin {
    /** COPPER-12: native outputs pose ordinal + 1 (standing 1, sitting 2, running 3, star 4). */
    @ModifyReturnValue(method = "getAnalogOutputSignal", at = @At("RETURN"))
    private int vbparity$poseSignal(int signal) {
        return signal + 1;
    }

    /** COPPER-12: native affectNeighborsAfterRemoval updates comparators when the statue goes away. */
    @Inject(method = "onRemove", at = @At("TAIL"))
    private void vbparity$updateComparators(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        if (!oldState.is(newState.getBlock())) {
            level.updateNeighbourForOutputSignal(pos, oldState.getBlock());
        }
    }
}
