package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VARIANTS-08/10/11/12: swaps in the native baby model (or the temperate cow model) for one {@code render} call, the way
 * native {@code AgeableMobRenderer} picks its adult or baby model. Layers see it through {@code getParentModel()}.
 * The swap runs inside VanillaBackport's own model swap, and only replaces its result for babies (which it is told to
 * leave alone) and for temperate cows (which it does not swap).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;
    @Unique
    private BabyRendering.Kit vbp$kit;
    @Unique
    private M vbp$swappedOut;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$createKit(EntityRendererProvider.Context context, M model, float shadowRadius, CallbackInfo ci) {
        this.vbp$kit = BabyRendering.create((LivingEntityRenderer<?, ?>) (Object) this, context, model);
    }

    @SuppressWarnings("unchecked")
    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void vbp$swapIn(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (this.vbp$kit != null && this.vbp$swappedOut == null) {
            EntityModel<?> chosen = this.vbp$kit.select(entity, this.model, partialTicks);
            if (chosen != null && chosen != this.model) {
                this.vbp$swappedOut = this.model;
                this.model = (M) chosen;
            }
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("RETURN")
    )
    private void vbp$swapOut(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (this.vbp$swappedOut != null) {
            this.model = this.vbp$swappedOut;
            this.vbp$swappedOut = null;
        }
    }
}
