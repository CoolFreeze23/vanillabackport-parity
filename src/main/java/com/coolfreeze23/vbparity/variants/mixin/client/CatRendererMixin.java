package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-11: kittens use the native {@code cat_<variant>_baby} texture. */
@Mixin(CatRenderer.class)
public abstract class CatRendererMixin {
    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Cat;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation vbp$babyTexture(ResourceLocation original, Cat entity) {
        ResourceLocation baby = BabyRendering.babyTexture(entity);
        return baby != null ? baby : original;
    }

    /**
     * Native 26.2 bakes the adult cat's 0.8 scale into its model ({@code AdultCatModel.CAT_TRANSFORMER}); the kitten model
     * is not scaled, so the 1.21.1 renderer-wide 0.8 is skipped for a kitten that gets the native model.
     */
    @WrapOperation(method = "scale(Lnet/minecraft/world/entity/animal/Cat;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void vbp$kittenScale(PoseStack poseStack, float x, float y, float z, Operation<Void> original, @Local(argsOnly = true) Cat entity) {
        if (BabyRendering.babyTexture(entity) == null) {
            original.call(poseStack, x, y, z);
        }
    }
}
