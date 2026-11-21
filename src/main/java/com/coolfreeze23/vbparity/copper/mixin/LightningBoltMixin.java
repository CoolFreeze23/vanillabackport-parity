package com.coolfreeze23.vbparity.copper.mixin;

import com.coolfreeze23.vbparity.copper.CopperLightning;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lightning strikes as in current vanilla. Both calls are wrapped at their call site in tick(), so VB's injections
 * inside powerLightningRod and clearCopperOnLightningStrike no longer run.
 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
    @Shadow
    protected abstract BlockPos getStrikePosition();

    /** COPPER-10: a single onLightningStrike per strike (VB's HEAD inject added a second call for its rods). */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LightningBolt;powerLightningRod()V"))
    private void vbparity$powerLightningRod(LightningBolt bolt, Operation<Void> original) {
        BlockPos strikePosition = this.getStrikePosition();
        Level level = bolt.level();
        BlockState stateBelow = level.getBlockState(strikePosition);
        if (stateBelow.getBlock() instanceof LightningRodBlock rod) {
            rod.onLightningStrike(stateBelow, level, strikePosition);
        }
    }

    /** COPPER-09: native clearCopperOnLightningStrike (struck rod resets itself; waxed targets still clean around). */
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LightningBolt;clearCopperOnLightningStrike(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void vbparity$clearCopper(Level level, BlockPos pos, Operation<Void> original) {
        CopperLightning.clearCopperOnLightningStrike(level, pos);
    }
}
