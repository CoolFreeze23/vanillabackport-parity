package com.coolfreeze23.vbparity.copper.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TBLK-102 / TBLK-151: native LightningRodBlock.onPlace keeps a powered rod placed without a scheduled tick (a struck
 * rod cleaned to lightning_rod, a scraped/waxed/oxidised powered rod, a piston-moved rod) powered and schedules the
 * 8-tick unpower tick. 1.21.1 unpowers it at once with flags 18, which skips every neighbour update.
 */
@Mixin(LightningRodBlock.class)
public abstract class LightningRodPlaceMixin {
    @WrapOperation(
        method = "onPlace",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private boolean vbparity$copper$keepPulse(Level level, BlockPos pos, BlockState unpowered, int flags, Operation<Boolean> original) {
        level.scheduleTick(pos, (Block) (Object) this, 8);
        return true;
    }
}
