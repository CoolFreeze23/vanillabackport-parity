package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-10: chicks use the native {@code chicken_<variant>_baby} texture. */
@Mixin(ChickenRenderer.class)
public abstract class ChickenRendererMixin {
    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Chicken;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation vbp$babyTexture(ResourceLocation original, Chicken entity) {
        ResourceLocation baby = BabyRendering.babyTexture(entity);
        return baby != null ? baby : original;
    }
}
