package com.coolfreeze23.vbparity.misc.mixin.client;

import com.blackgear.vanillabackport.client.api.modules.music_toast.ToastInstance;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Unique;
import java.lang.reflect.Field;

import com.coolfreeze23.vbparity.misc.client.MusicToastState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toasts render after the screen, so the pause-screen render flag is always clear here (whatever the hook order is). */
@Mixin(ToastComponent.class)
public abstract class ToastComponentPauseFlagMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void vbp$leavePauseRender(GuiGraphics graphics, CallbackInfo ci) {
        MusicToastState.inPauseScreenRender = false;
        // Native ToastManager.update runs every frame, also while the HUD is hidden (F1); VB only updates the
        // "Now Playing" toast from its render hook, which returns early when the HUD is hidden.
        if (Minecraft.getInstance().options.hideGui) {
            Object toast = vbp$nowPlayingToast((ToastComponent) (Object) this);
            if (toast instanceof ToastInstance<?> instance) {
                instance.update();
            }
        }
    }

    @Unique
    private static Field vbp$nowPlayingField;
    @Unique
    private static boolean vbp$fieldLookedUp;

    @Unique
    private static Object vbp$nowPlayingToast(ToastComponent component) {
        if (!vbp$fieldLookedUp) {
            vbp$fieldLookedUp = true;
            for (Field field : ToastComponent.class.getDeclaredFields()) {
                if (field.getType() == ToastInstance.class) {
                    field.setAccessible(true);
                    vbp$nowPlayingField = field;
                    break;
                }
            }
        }
        try {
            return vbp$nowPlayingField != null ? vbp$nowPlayingField.get(component) : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
