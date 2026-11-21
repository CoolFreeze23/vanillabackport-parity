package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.EndClock;

import com.blackgear.vanillabackport.client.api.modules.end_flashes.EndFlashRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndFlashRenderer.class)
public abstract class EndFlashRendererMixin {
    /** The End flash tint now goes through the lightmap sky term (LightTextureEndMixin), like native. */
    @Inject(method = "applyLightTint", at = @At("HEAD"), cancellable = true)
    private static void vbp$noPostTint(NativeImage lightPixels, float intensity, CallbackInfo ci) {
        ci.cancel();
    }

    /** Native SkyRenderer draws the End flash regardless of "Hide Lightning Flashes" (only terrain lighting obeys it). */
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private static Object vbp$skyFlashIgnoresOption(Object hideLightningFlash) {
        return Boolean.FALSE;
    }

    /**
     * Native ticks the flash state with the End world clock (ClientLevel: {@code endFlashState.tick(getDefaultClockTime())}).
     * The add-on ports that clock (EndClock, synced from the server); when the server does not send it, the shared day
     * time is the closest stand-in (it also stops with doDaylightCycle=false).
     */
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getGameTime()J"))
    private static long vbp$flashClock(long gameTime, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ClientLevel level) {
        return EndClock.ClientState.isSynced() ? EndClock.ClientState.tick(gameTime) : level.getDayTime();
    }
}
