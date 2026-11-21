package com.coolfreeze23.vbparity.wild.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WILD-21 (and TBLK-301 event 1052). Native LevelEventHandler case 2014: addDestroyBlockEffect only, no break sound (1.21.1 has no event 2014). */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$strawBedDestroyParticles(int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type == com.coolfreeze23.vbparity.sulfur.SulfurSounds.SULFUR_SPIKE_LAND_EVENT) {
            // TBLK-301: native LevelEventHandler case 1052
            this.level.playLocalSound(pos, com.coolfreeze23.vbparity.sulfur.SulfurSounds.SULFUR_SPIKE_LAND, net.minecraft.sounds.SoundSource.BLOCKS,
                2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            ci.cancel();
        } else if (type == 2014) {
            this.level.addDestroyBlockEffect(pos, Block.stateById(data));
            ci.cancel();
        }
    }
}
