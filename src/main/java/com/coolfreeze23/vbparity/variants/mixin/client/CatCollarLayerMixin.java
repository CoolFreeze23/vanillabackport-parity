package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.coolfreeze23.vbparity.variants.client.model.BabyCatModel;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VARIANTS-11: a kitten drawn with the native baby model wears the native baby collar ({@code CAT_BABY_COLLAR}, the
 * kitten mesh scaled 1.01) with {@code cat_collar_baby.png}, as native {@code CatCollarLayer}.
 */
@Mixin(CatCollarLayer.class)
public abstract class CatCollarLayerMixin {
    @Unique
    private CatModel<Cat> vbp$babyCollar;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$bakeBabyCollar(RenderLayerParent<Cat, CatModel<Cat>> renderer, EntityModelSet modelSet, CallbackInfo ci) {
        this.vbp$babyCollar = new BabyCatModel(modelSet.bakeLayer(BabyRendering.CAT_BABY_COLLAR), 1.01F);
    }

    @ModifyExpressionValue(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Cat;FFFFFF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/CatCollarLayer;catModel:Lnet/minecraft/client/model/CatModel;")
    )
    private CatModel<Cat> vbp$babyCollarModel(CatModel<Cat> original, @Local(argsOnly = true) Cat cat) {
        return BabyRendering.babyTexture(cat) != null ? this.vbp$babyCollar : original;
    }

    @ModifyExpressionValue(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Cat;FFFFFF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/CatCollarLayer;CAT_COLLAR_LOCATION:Lnet/minecraft/resources/ResourceLocation;")
    )
    private ResourceLocation vbp$babyCollarTexture(ResourceLocation original, @Local(argsOnly = true) Cat cat) {
        return BabyRendering.babyTexture(cat) != null ? BabyRendering.CAT_COLLAR_BABY_TEXTURE : original;
    }
}
