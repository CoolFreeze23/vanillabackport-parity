package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-11: puppies use the native {@code wolf_<variant>[_tame|_angry]_baby} texture. */
@Mixin(WolfRenderer.class)
public abstract class WolfRendererMixin {
    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Wolf;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation vbp$babyTexture(ResourceLocation original, Wolf entity) {
        ResourceLocation baby = BabyRendering.babyTexture(entity);
        return baby != null ? baby : original;
    }
}
