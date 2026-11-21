package com.coolfreeze23.vbparity.devcheck.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev-only: keeps the check client's window off the user's screen. */
@Mixin(Window.class)
public abstract class WindowHideMixin {
    @Shadow @Final private long window;

    @Inject(method = "updateDisplay", at = @At("TAIL"))
    private void vbparity$devcheck$stayHidden(CallbackInfo ci) {
        if (Boolean.getBoolean("vbparity.devcheck") && GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE) {
            GLFW.glfwHideWindow(this.window);
        }
    }
}
