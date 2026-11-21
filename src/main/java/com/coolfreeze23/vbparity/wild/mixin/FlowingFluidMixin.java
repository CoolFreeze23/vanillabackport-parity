package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.common.level.blocks.StrawBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WILD-17. Native FlowingFluid.canHoldFluid is true for #washed_away_by_fluids, which has minecraft:straw_bed. In 1.21.1
 * the straw bed's 16x4x16 collision counts as motion-blocking, so fluids treat it as a wall. Red shrub and shelf mushroom
 * are already washed away in VB.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "canHoldFluid", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$strawBedWashedAway(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof StrawBedBlock) {
            cir.setReturnValue(true);
        }
    }
}
