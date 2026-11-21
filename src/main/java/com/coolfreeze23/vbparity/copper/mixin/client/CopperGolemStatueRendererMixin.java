package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.renderer.block_entity.CopperGolemStatueRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** COPPER-26: the native statue model renders with entityCutout (back faces culled). */
@Mixin(CopperGolemStatueRenderer.class)
public abstract class CopperGolemStatueRendererMixin {
    @WrapOperation(
        method = "render(Lcom/blackgear/vanillabackport/common/level/block_entities/CopperGolemStatueBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private RenderType vbparity$cullBackFaces(ResourceLocation texture, Operation<RenderType> original) {
        return RenderType.entityCutout(texture);
    }
}
