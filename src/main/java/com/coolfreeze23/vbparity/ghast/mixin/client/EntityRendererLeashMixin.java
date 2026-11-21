package com.coolfreeze23.vbparity.ghast.mixin.client;

import com.coolfreeze23.vbparity.ghast.LeashRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GHAST-40: exactly one lead per connection. With modern leashes VB's lead renderer already draws every lead, so the
 * old 1.21.1 lead is not drawn as well. With modern leashes off, the old lead is drawn (VB's wrapper skips it). This
 * wrapper is registered at a higher priority so it wraps VB's and decides first.
 */
@Mixin(value = EntityRenderer.class, priority = 1100)
public abstract class EntityRendererLeashMixin<T extends Entity> {
    @Shadow
    private <E extends Entity> void renderLeash(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, E leashHolder) {
        throw new AssertionError();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderLeash(Lnet/minecraft/world/entity/Entity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;)V"))
    private void vbp$oneLeadOnly(EntityRenderer<T> instance, T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, Entity leashHolder,
                                 Operation<Void> original) {
        if (!LeashRules.modernLeashes()) {
            this.renderLeash(entity, partialTick, poseStack, bufferSource, leashHolder);
        }
    }
}
