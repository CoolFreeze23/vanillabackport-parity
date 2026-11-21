package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_toast.MusicToastAccess;
import com.blackgear.vanillabackport.client.api.modules.music_toast.ToastInstance;
import com.coolfreeze23.vbparity.misc.client.MusicToastState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native Options sound sliders: raising MUSIC or MASTER above 0 shows the toast of a song that started muted. */
@Mixin(SoundManager.class)
public abstract class SoundManagerToastMixin {
    @Inject(method = "updateSourceVolume", at = @At("TAIL"))
    private void vbp$showPendingToast(SoundSource category, float volume, CallbackInfo ci) {
        if ((category == SoundSource.MUSIC || category == SoundSource.MASTER) && MusicToastState.toastPending) {
            Minecraft minecraft = Minecraft.getInstance();
            if (ToastInstance.getFinalSoundSourceVolume(minecraft.options, SoundSource.MUSIC) > 0.0F) {
                ((MusicToastAccess) minecraft.getToasts()).showNowPlayingToast();
            }
        }
    }
}
