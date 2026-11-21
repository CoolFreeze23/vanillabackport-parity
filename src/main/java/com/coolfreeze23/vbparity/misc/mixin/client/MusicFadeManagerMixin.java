package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_fade.MusicFadeManager;
import com.blackgear.vanillabackport.core.mixin.client.access.MusicManagerAccessor;
import com.blackgear.vanillabackport.core.mixin.client.access.SoundManagerAccessor;
import com.coolfreeze23.vbparity.misc.client.MusicGainAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Native MusicManager keeps its fade gain across songs (a song started after a fade-out swells in from near
 * silence), and while a fade is in progress the rest of the music tick still runs; it only returns when the fade
 * stopped the song.
 */
@Mixin(MusicFadeManager.class)
public abstract class MusicFadeManagerMixin {
    @Shadow
    @Final
    private MusicManager manager;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private float currentGain;

    @Shadow
    private float getTargetVolume() {
        throw new AssertionError();
    }

    @Shadow
    private boolean fadePlaying(float targetVolume, SoundInstance currentMusic) {
        throw new AssertionError();
    }

    /** Native startPlaying does not touch the gain: the new song plays at the current (engine-wide) fade gain. */
    @Inject(method = "onStartPlaying", at = @At("HEAD"), cancellable = true)
    private void vbp$keepGain(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Native applies the fade as a MUSIC category gain inside the sound engine (updateCategoryVolume), so it also
     * holds when the music slider is moved and for the next song. VB set one channel's volume, which the slider reset.
     */
    @Inject(method = "setVolume", at = @At("HEAD"), cancellable = true)
    private void vbp$categoryGain(SoundEngine engine, SoundInstance instance, float volume, CallbackInfo ci) {
        ((MusicGainAccess) engine).vbp$setMusicGain(volume);
        ci.cancel();
    }

    /** VB's caller cancels the music tick when this returns true; native returns only when the fade stopped the song. */
    @Inject(method = "tickFade", at = @At("HEAD"), cancellable = true)
    private void vbp$nativeFadeFlow(SoundInstance currentMusic, CallbackInfoReturnable<Boolean> cir) {
        if (currentMusic == null) {
            cir.setReturnValue(false);
            return;
        }
        float target = this.getTargetVolume();
        if (this.currentGain == target) {
            cir.setReturnValue(false);
            return;
        }
        boolean stillPlaying = this.fadePlaying(target, currentMusic);
        cir.setReturnValue(!stillPlaying);
    }
}
