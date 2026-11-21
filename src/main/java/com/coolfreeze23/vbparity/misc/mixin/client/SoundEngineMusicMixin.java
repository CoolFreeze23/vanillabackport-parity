package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.client.MusicGainAccess;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Listener;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;

/**
 * Native SoundEngine keeps music running at zero volume: a song may start silently ({@code STARTED_SILENTLY}),
 * a zero music slider no longer stops it, and raising the slider makes it audible mid-track. While the game is
 * paused, finished music channels are still cleaned up ({@code tickMusicWhenPaused}) so the next song can start.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMusicMixin implements MusicGainAccess {
    /** Native {@code gainBySource.get(MUSIC)}: the biome fade gain, applied to every music sound in calculateVolume. */
    @Unique
    private float vbp$musicGain = 1.0F;

    @Override
    public void vbp$setMusicGain(float gain) {
        this.vbp$musicGain = Mth.clamp(gain, 0.0F, 1.0F);
        if (this.loaded) {
            // Native refreshCategoryVolume(MUSIC).
            this.instanceToChannel.forEach((sound, handle) -> {
                if (sound.getSource() == SoundSource.MUSIC) {
                    float newVolume = this.calculateVolume(sound);
                    handle.execute(channel -> channel.setVolume(newVolume));
                }
            });
        }
    }

    @ModifyReturnValue(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"))
    private float vbp$applyMusicGain(float volume, float volumeMultiplier, SoundSource source) {
        return source == SoundSource.MUSIC ? volume * this.vbp$musicGain : volume;
    }

    /** Native stopAll clears gainBySource. */
    @Inject(method = "stopAll", at = @At("TAIL"))
    private void vbp$resetMusicGain(CallbackInfo ci) {
        this.vbp$musicGain = 1.0F;
    }

    @Shadow
    private boolean loaded;

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow
    @Final
    private Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow
    private float calculateVolume(SoundInstance sound) {
        throw new AssertionError();
    }

    @WrapOperation(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;canStartSilent()Z"))
    private boolean vbp$musicStartsSilently(SoundInstance sound, Operation<Boolean> original) {
        return original.call(sound) || sound.getSource() == SoundSource.MUSIC;
    }

    @WrapOperation(method = "play", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;getGain()F"))
    private float vbp$musicIgnoresMasterGainCheck(Listener listener, Operation<Float> original, @Local(argsOnly = true) SoundInstance sound) {
        return sound.getSource() == SoundSource.MUSIC ? 1.0F : original.call(listener);
    }

    @WrapOperation(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private float vbp$keepSilentMusic(Options options, SoundSource source, Operation<Float> original) {
        return source == SoundSource.MUSIC ? 1.0F : original.call(options, source);
    }

    @Inject(method = "updateCategoryVolume", at = @At("HEAD"), cancellable = true)
    private void vbp$silenceInsteadOfStoppingMusic(SoundSource category, float volume, CallbackInfo ci) {
        if (this.loaded && category != SoundSource.MASTER) {
            this.instanceToChannel.forEach((sound, handle) -> {
                float newVolume = this.calculateVolume(sound);
                boolean music = sound.getSource() == SoundSource.MUSIC;
                handle.execute(channel -> {
                    if (newVolume <= 0.0F && !music) {
                        channel.stop();
                    } else {
                        channel.setVolume(newVolume);
                    }
                });
            });
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vbp$tickMusicWhenPaused(boolean isGamePaused, CallbackInfo ci) {
        if (!isGamePaused || !this.loaded) {
            return;
        }
        Iterator<Map.Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            SoundInstance sound = entry.getKey();
            if (sound.getSource() == SoundSource.MUSIC && entry.getValue().isStopped()) {
                iterator.remove();
                this.soundDeleteTime.remove(sound);
                this.instanceBySource.remove(sound.getSource(), sound);
            }
        }
    }
}
