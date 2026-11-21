package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.end_flashes.DirectionalSoundInstance;
import net.minecraft.client.Camera;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native DirectionalSoundInstance sets its position (10 blocks along the flash direction, no attenuation) in the
 * constructor. Without it the delayed sound starts at the world origin with linear attenuation fixed for its channel.
 */
@Mixin(DirectionalSoundInstance.class)
public abstract class DirectionalSoundInstanceMixin {
    @Shadow
    private void setPosition() {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$positionNow(SoundEvent sound, SoundSource source, RandomSource random, Camera camera, float xAngle, float yAngle, CallbackInfo ci) {
        this.setPosition();
    }
}
