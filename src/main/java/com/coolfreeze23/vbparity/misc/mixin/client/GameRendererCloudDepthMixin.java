package com.coolfreeze23.vbparity.misc.mixin.client;

import com.coolfreeze23.vbparity.misc.client.CloudRangeAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native Camera: {@code depthFar = max(renderDistance * 4, cloudRange * 16)}, so far clouds are not clipped. */
@Mixin(GameRenderer.class)
public abstract class GameRendererCloudDepthMixin {
    @ModifyReturnValue(method = "getDepthFar", at = @At("RETURN"))
    private float vbp$cloudDepthFar(float depthFar) {
        return CloudRangeAccess.nativeClouds() ? Math.max(depthFar, CloudRangeAccess.cloudRange() * 16.0F) : depthFar;
    }
}
