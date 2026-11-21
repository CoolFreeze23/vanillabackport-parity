package com.coolfreeze23.vbparity.variants.mixin.client;

import com.coolfreeze23.vbparity.variants.client.BabyRendering;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** VARIANTS-12: lambs use the native {@code sheep_baby} texture. */
@Mixin(SheepRenderer.class)
public abstract class SheepRendererMixin {
    @ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Sheep;)Lnet/minecraft/resources/ResourceLocation;", at = @At("RETURN"))
    private ResourceLocation vbp$babyTexture(ResourceLocation original, Sheep entity) {
        ResourceLocation baby = entity.isBaby() ? BabyRendering.SHEEP_BABY_TEXTURE : null;
        return baby != null ? baby : original;
    }
}
