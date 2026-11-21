package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_frequency.MusicFrequency;
import com.blackgear.vanillabackport.client.api.modules.options.OptionsAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native MusicManager.tick: when a song ends, {@code nextSongDelay = min(nextSongDelay, frequency.getNextSongDelay(music))},
 * one draw. VB first lowers the delay with its own draw (right after isActive) and then 1.21.1 takes the min with a
 * second, unrelated draw. Here the value from before VB's step is used, with the single native draw.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerFrequencyMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int nextSongDelay;

    @Unique
    private int vbp$delayBeforeSongEnd;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;isActive(Lnet/minecraft/client/resources/sounds/SoundInstance;)Z"))
    private void vbp$rememberDelay(CallbackInfo ci) {
        this.vbp$delayBeforeSongEnd = this.nextSongDelay;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/lang/Math;min(II)I"))
    private int vbp$singleDraw(int currentDelay, int draw, Operation<Integer> original) {
        return original.call(this.vbp$delayBeforeSongEnd, draw);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))
    private int vbp$frequencyDraw(RandomSource random, int min, int max, Operation<Integer> original, @Local Music music) {
        MusicFrequency frequency = MusicFrequency.DEFAULT;
        if (this.minecraft.options instanceof OptionsAccess access) {
            OptionInstance<MusicFrequency> option = access.musicFrequency();
            if (option != null) {
                frequency = option.get();
            }
        }
        return frequency.getNextSongDelay(music, random);
    }
}
