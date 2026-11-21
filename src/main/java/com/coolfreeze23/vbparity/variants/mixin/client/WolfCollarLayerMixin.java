package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-11: a puppy drawn with the native baby model wears {@code wolf_collar_baby.png}, as native {@code WolfCollarLayer}. */
@Mixin(WolfCollarLayer.class)
public abstract class WolfCollarLayerMixin {
    @ModifyExpressionValue(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Wolf;FFFFFF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/WolfCollarLayer;WOLF_COLLAR_LOCATION:Lnet/minecraft/resources/ResourceLocation;")
    )
    private ResourceLocation vbp$babyCollar(ResourceLocation original, @Local(argsOnly = true) Wolf wolf) {
        return BabyRendering.babyTexture(wolf) != null ? BabyRendering.WOLF_COLLAR_BABY_TEXTURE : original;
    }
}
