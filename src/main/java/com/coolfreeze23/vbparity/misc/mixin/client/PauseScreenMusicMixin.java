package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_toast.MusicToastDisplayState;
import com.blackgear.vanillabackport.client.api.modules.music_toast.NowPlayingToast;
import com.blackgear.vanillabackport.client.api.modules.music_toast.ToastInstance;
import com.blackgear.vanillabackport.client.api.modules.options.OptionsAccess;
import com.coolfreeze23.vbparity.misc.client.MusicToastState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native PauseScreen.tick: {@code if (rendersNowPlayingToast()) NowPlayingToast.tickMusicNotes();}. VB calls it from
 * render (every frame); that call is suppressed while render runs (the flag is cleared again when the toasts render,
 * which happens after the screen, so this does not depend on the order of the TAIL hooks).
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMusicMixin extends Screen {
    @Shadow
    @Final
    private boolean showPauseMenu;

    protected PauseScreenMusicMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void vbp$enterRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        MusicToastState.inPauseScreenRender = true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vbp$tickMusicNotes(CallbackInfo ci) {
        MusicToastState.inPauseScreenRender = false;
        if (this.minecraft != null && this.minecraft.options instanceof OptionsAccess access && access.musicToast() != null
                && ((MusicToastDisplayState) access.musicToast().get()).renderInPauseScreen()
                && ToastInstance.getFinalSoundSourceVolume(this.minecraft.options, SoundSource.MUSIC) > 0.0F
                && this.showPauseMenu) {
            NowPlayingToast.tickMusicNotes();
        }
    }
}
