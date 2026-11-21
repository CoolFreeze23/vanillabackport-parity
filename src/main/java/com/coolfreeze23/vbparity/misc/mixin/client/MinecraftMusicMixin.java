package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.core.mixin.client.access.SoundEngineAccessor;
import com.blackgear.vanillabackport.core.mixin.client.access.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Minecraft.class)
public abstract class MinecraftMusicMixin {
    @Shadow
    private volatile boolean pause;

    @Shadow
    @Final
    private MusicManager musicManager;

    @Shadow
    @Final
    private SoundManager soundManager;

    /** Native Minecraft.tick runs {@code musicManager.tick()} outside {@code if (!this.pause)}; 1.21.1 only runs it unpaused. */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;tick(Z)V"))
    private void vbp$tickMusicWhilePaused(CallbackInfo ci) {
        if (this.pause) {
            this.musicManager.tick();
        }
    }

    /**
     * Native pauseGame: {@code soundManager.pauseAllExcept(MUSIC, UI)}. VB pauses (and mutes) UI sounds too; let them
     * play on. 1.21.1 has no UI category: its UI sounds (SimpleSoundInstance.forUI: clicks, toasts) use MASTER.
     */
    @Inject(method = "pauseGame", at = @At("TAIL"))
    private void vbp$keepUiSoundsPlaying(boolean pauseOnly, CallbackInfo ci) {
        SoundEngineAccessor engine = (SoundEngineAccessor) ((SoundManagerAccessor) this.soundManager).getSoundEngine();
        if (!engine.isLoaded()) {
            return;
        }
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : engine.getInstanceToChannel().entrySet()) {
            SoundInstance sound = entry.getKey();
            if (sound.getSource() == SoundSource.MASTER) {
                float volume = engine.callCalculateVolume(sound);
                entry.getValue().execute(channel -> {
                    channel.setVolume(volume);
                    channel.unpause();
                });
            }
        }
    }
}
