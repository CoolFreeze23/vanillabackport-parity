package com.coolfreeze23.vbparity.garden.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_fade.MusicFadeManager;
import com.coolfreeze23.vbparity.garden.client.GardenMusic;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GARDEN-20: the pale garden's native audio/music_volume and audio/background_music, sampled around the camera the way
 * 26.2's EnvironmentAttributeProbe does it (see GardenMusic). The fade itself (persistent category gain, the rest of the
 * tick running during a fade) is the misc area's MusicFadeManagerMixin, which reads getTargetVolume through VB.
 */
@Mixin(MusicFadeManager.class)
public abstract class MusicFadeManagerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "getTargetVolume", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$blendedVolume(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(GardenMusic.targetVolume(this.minecraft));
    }

    @Inject(method = "isSilentBiome", at = @At("HEAD"), cancellable = true)
    private void vbparity$garden$silentAtCamera(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(GardenMusic.isSilent(this.minecraft));
    }
}
