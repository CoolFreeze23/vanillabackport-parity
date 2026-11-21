package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-10: calves use the native {@code cow_<variant>_baby} texture. */
@Mixin(CowRenderer.class)
public abstract class CowRendererMixin {
    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Cow;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation vbp$babyTexture(ResourceLocation original, Cow entity) {
        ResourceLocation baby = BabyRendering.babyTexture(entity);
        return baby != null ? baby : original;
    }
}
