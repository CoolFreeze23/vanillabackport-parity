package com.coolfreeze23.vbparity.flora.mixin;

import com.blackgear.vanillabackport.client.level.sound.AmbientDesertBlockSoundsPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FLORA-02: native SandBlock.animateTick runs the falling-dust roll, then the sand idle sound. VB hooks the sound onto
 * Block.animateTick, which FallingBlock overrides without calling super, so it never ran for sand.
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {
    @Inject(method = "animateTick", at = @At("TAIL"))
    private void vbparity$sandIdleSound(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            AmbientDesertBlockSoundsPlayer.playAmbientSandSounds(level, pos, random);
        }
    }
}
