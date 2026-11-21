package com.coolfreeze23.vbparity.ghast.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.entity.mob.HappyGhastRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** GHAST-19: the happy ghast shadow radius is 2.0 (scaled down for ghastlings by the age scale). */
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/MobRenderer;<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/client/model/EntityModel;F)V"), index = 2)
    private static float vbp$shadowRadius(float radius) {
        return 2.0F;
    }
}
