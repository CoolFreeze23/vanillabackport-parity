package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.client.CloudRangeAccess;
import com.coolfreeze23.vbparity.misc.client.NativeCloudRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With VB's extended cloud reach on, the clouds are drawn by the port of the native renderer (NativeCloudRenderer)
 * with the native Cloud Distance option. Dimension effects that draw their own clouds (NeoForge hook at the start of
 * renderClouds) keep doing so.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererNativeCloudsMixin {
    @Inject(method = "renderClouds", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private void vbp$nativeClouds(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
                                  double camX, double camY, double camZ, CallbackInfo ci) {
        if (CloudRangeAccess.nativeClouds()
                && NativeCloudRenderer.INSTANCE.render(frustumMatrix, projectionMatrix, partialTick, camX, camY, camZ, CloudRangeAccess.cloudRange())) {
            ci.cancel();
        }
    }
}
