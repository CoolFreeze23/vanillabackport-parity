package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.coolfreeze23.vbparity.variants.client.model.BabySheepModels;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VARIANTS-12: lambs grow the native baby wool ({@code SHEEP_BABY_WOOL} with {@code sheep_wool_baby.png}).
 * VARIANTS-16: jeb_ sheep cycle colours from their age alone, as native {@code ColorLerper} (no entity-id offset).
 */
@Mixin(SheepFurLayer.class)
public abstract class SheepFurLayerMixin {
    @Unique
    private static final String RENDER =
        "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Sheep;FFFFFF)V";

    @Unique
    private SheepFurModel<Sheep> vbp$babyWool;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void vbp$bakeBabyWool(RenderLayerParent<Sheep, SheepModel<Sheep>> renderer, EntityModelSet modelSet, CallbackInfo ci) {
        this.vbp$babyWool = new BabySheepModels.Wool(modelSet.bakeLayer(BabyRendering.SHEEP_BABY_WOOL));
    }

    @ModifyExpressionValue(method = RENDER, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/SheepFurLayer;model:Lnet/minecraft/client/model/SheepFurModel;"))
    private SheepFurModel<Sheep> vbp$babyWoolModel(SheepFurModel<Sheep> original, @Local(argsOnly = true) Sheep sheep) {
        return sheep.isBaby() ? this.vbp$babyWool : original;
    }

    @ModifyExpressionValue(method = RENDER, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/SheepFurLayer;SHEEP_FUR_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation vbp$babyWoolTexture(ResourceLocation original, @Local(argsOnly = true) Sheep sheep) {
        return sheep.isBaby() ? BabyRendering.SHEEP_BABY_WOOL_TEXTURE : original;
    }

    @ModifyExpressionValue(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;getId()I"))
    private int vbp$noIdOffset(int original) {
        return 0;
    }
}
