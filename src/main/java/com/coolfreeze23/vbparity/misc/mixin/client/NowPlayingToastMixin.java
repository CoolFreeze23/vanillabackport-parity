package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_toast.NowPlayingToast;
import com.blackgear.vanillabackport.client.api.modules.music_toast.ToastInstance;
import com.coolfreeze23.vbparity.misc.client.MusicToastState;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NowPlayingToast.class)
public abstract class NowPlayingToastMixin {
    /** Native MusicManager.startPlaying shows the toast only for STARTED, not STARTED_SILENTLY (music volume 0). */
    @Inject(method = "showToast", at = @At("HEAD"), cancellable = true)
    private void vbp$notWhileMuted(Options options, CallbackInfo ci) {
        if (ToastInstance.getFinalSoundSourceVolume(options, SoundSource.MUSIC) <= 0.0F) {
            MusicToastState.toastPending = true;
            ci.cancel();
        } else {
            MusicToastState.toastPending = false;
        }
    }

    /** Native PauseScreen ticks the note colour once per game tick; VB also ticked it every rendered frame. */
    @Inject(method = "tickMusicNotes", at = @At("HEAD"), cancellable = true)
    private static void vbp$notFromPauseRender(CallbackInfo ci) {
        if (MusicToastState.inPauseScreenRender) {
            ci.cancel();
        }
    }

    /** Native key: {@code location.toShortLanguageKey()} with "/" replaced by "." and no fallback text. */
    @Inject(method = "getNowPlayingString", at = @At("HEAD"), cancellable = true)
    private static void vbp$nativeKey(ResourceLocation location, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(location == null ? Component.empty() : Component.translatable(location.toShortLanguageKey().replace("/", ".")));
    }
}
